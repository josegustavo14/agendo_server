package agendo.app.server.modules.payment.service;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.payment.client.AbacatePayClient;
import agendo.app.server.modules.payment.dto.request.CreateBillingRequest;
import agendo.app.server.modules.payment.dto.request.CreateCustomerRequest;
import agendo.app.server.modules.payment.dto.response.BillingListResponse;
import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.dto.response.CustomerListResponse;
import agendo.app.server.modules.payment.dto.response.CustomerResponse;
import agendo.app.server.modules.payment.models.PaymentEntity;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.modules.user.models.UserEntity;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AbacatePayClient abacatePayClient;
    private final PaymentRepository paymentRepository;

    @Value("${abacatepay.return-url}")
    private String returnUrl;

    @Value("${abacatepay.completion-url}")
    private String completionUrl;

    public CustomerResponse createCustomer(UserEntity user) {

        String taxId = user.getClientProfile() != null
                ? user.getClientProfile().getTaxId()
                : null;

        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .name(user.getName())
                .email(user.getEmail())
                .cellphone(user.getPhone())
                .taxId(taxId)
                .build();
        return abacatePayClient.createCustomer(request);
    }

    public CustomerListResponse listCustomers() {
        return abacatePayClient.listCustomers();
    }

    /**
     * cria uma cobrança na AbacatePay e persiste o registro localmente.
     * evita cobranças duplicadas para o mesmo agendamento.
     */
    @Transactional
    public BillingResponse createBillingForAppointment(
            AppointmentEntity appointment,
            UserEntity user,
            int priceInCents
    ) {

        String taxId = user.getClientProfile() != null
                ? user.getClientProfile().getTaxId()
                : null;

        // idempotência: bloqueia cobrança duplicada pro mesmo agendamento
        paymentRepository.findByAppointmentId(appointment.getId()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Já existe uma cobrança para o agendamento " + appointment.getId()
                            + " (billingId=" + existing.getAbacatePayBillingId()
                            + ", status=" + existing.getStatus() + ")"
            );
        });

        CreateBillingRequest.ProductItem product = CreateBillingRequest.ProductItem.builder()
                .externalId("appointment-" + appointment.getId())
                .name("Agendamento #" + appointment.getId())
                .description("Serviço agendado via Agendo")
                .quantity(1)
                .price(priceInCents)
                .build();

        CreateBillingRequest.CustomerData customer = CreateBillingRequest.CustomerData.builder()
                .name(user.getName())
                .email(user.getEmail())
                .cellphone(user.getPhone())
                .taxId(taxId)
                .build();

        CreateBillingRequest request = CreateBillingRequest.builder()
                .frequency("ONE_TIME")
                .methods(List.of("PIX"))
                .products(List.of(product))
                .returnUrl(returnUrl)
                .completionUrl(completionUrl)
                .customer(customer)
                .allowCoupons(false)
                .externalId("agendo-appt-" + appointment.getId())
                .build();

        BillingResponse response = abacatePayClient.createBilling(request);

        // persiste localmente para o webhook conseguir atualizar o status depois
        PaymentEntity payment = PaymentEntity.builder()
                .abacatePayBillingId(response.getData().getId())
                .paymentUrl(response.getData().getUrl())
                .status("PENDING")
                .amountInCents(priceInCents)
                .appointment(appointment)
                .build();

        paymentRepository.save(payment);
        log.info("[Payment] Cobrança criada: billingId={} | url={}",
                payment.getAbacatePayBillingId(), payment.getPaymentUrl());

        return response;
    }

    public BillingListResponse listBillings() {
        return abacatePayClient.listBillings();
    }
}