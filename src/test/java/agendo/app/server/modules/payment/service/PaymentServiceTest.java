package agendo.app.server.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.payment.client.AbacatePayClient;
import agendo.app.server.modules.payment.dto.request.CreateBillingRequest;
import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.models.PaymentEntity;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.modules.user.models.ClientProfileEntity;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;

/**
 * Testes unitários do PaymentService.
 *
 * O AbacatePayClient é um FeignClient — ou seja, em tempo de execução real
 * ele faz uma chamada HTTP de verdade para a API da AbacatePay. Nos testes
 * unitários isso NUNCA deve acontecer: usamos Mockito para criar um
 * AbacatePayClient falso (@Mock) e controlar exatamente o que ele retorna,
 * sem nenhuma chamada de rede.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AbacatePayClient abacatePayClient;

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    private UserEntity client;
    private AppointmentEntity appointment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(abacatePayClient, paymentRepository);

        // @Value não é injetado fora do contexto Spring — setamos manualmente
        ReflectionTestUtils.setField(paymentService, "returnUrl", "http://localhost/cancelado");
        ReflectionTestUtils.setField(paymentService, "completionUrl", "http://localhost/sucesso");

        ClientProfileEntity profile = ClientProfileEntity.builder().taxId("123.456.789-00").build();
        client = UserEntity.builder()
                .id(7L).name("Ana Cliente").email("ana@teste.com").phone("11999990000")
                .role(UserRole.CLIENT).clientProfile(profile)
                .build();

        appointment = AppointmentEntity.builder().id(42L).build();
    }

    @Test
    void createBillingForAppointment_deveChamarGatewayEPersistirCobranca() {
        BillingResponse stub = stubBillingResponse("bill_abc123", "https://pay.abacatepay.com/bill_abc123");

        when(paymentRepository.findByAppointmentId(42L)).thenReturn(Optional.empty());
        when(abacatePayClient.createBilling(any())).thenReturn(stub);

        BillingResponse response = paymentService.createBillingForAppointment(appointment, client, 5000);

        assertThat(response.getData().getId()).isEqualTo("bill_abc123");

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(captor.capture());

        PaymentEntity saved = captor.getValue();
        assertThat(saved.getAbacatePayBillingId()).isEqualTo("bill_abc123");
        assertThat(saved.getPaymentUrl()).isEqualTo("https://pay.abacatepay.com/bill_abc123");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getAmountInCents()).isEqualTo(5000);
        assertThat(saved.getAppointment()).isEqualTo(appointment);
    }

    @Test
    void createBillingForAppointment_deveEnviarTaxIdDoPerfilDoCliente() {
        when(paymentRepository.findByAppointmentId(42L)).thenReturn(Optional.empty());
        when(abacatePayClient.createBilling(any())).thenReturn(stubBillingResponse("bill_x", "https://pay/x"));

        paymentService.createBillingForAppointment(appointment, client, 5000);

        ArgumentCaptor<CreateBillingRequest> captor = ArgumentCaptor.forClass(CreateBillingRequest.class);
        verify(abacatePayClient).createBilling(captor.capture());

        CreateBillingRequest request = captor.getValue();
        assertThat(request.getCustomer().getTaxId()).isEqualTo("123.456.789-00");
        assertThat(request.getExternalId()).isEqualTo("agendo-appt-42");
        assertThat(request.getProducts()).hasSize(1);
        assertThat(request.getProducts().get(0).getPrice()).isEqualTo(5000);
        assertThat(request.getMethods()).containsExactly("PIX");
    }

    @Test
    void createBillingForAppointment_clienteSemPerfilEnviaTaxIdNulo() {
        client.setClientProfile(null);

        when(paymentRepository.findByAppointmentId(42L)).thenReturn(Optional.empty());
        when(abacatePayClient.createBilling(any())).thenReturn(stubBillingResponse("bill_y", "https://pay/y"));

        paymentService.createBillingForAppointment(appointment, client, 3000);

        ArgumentCaptor<CreateBillingRequest> captor = ArgumentCaptor.forClass(CreateBillingRequest.class);
        verify(abacatePayClient).createBilling(captor.capture());

        assertThat(captor.getValue().getCustomer().getTaxId()).isNull();
    }

    @Test
    void createBillingForAppointment_cobrancaDuplicadaLancaExcecaoSemChamarGateway() {
        PaymentEntity existing = PaymentEntity.builder()
                .abacatePayBillingId("bill_existente").status("PENDING").build();

        when(paymentRepository.findByAppointmentId(42L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.createBillingForAppointment(appointment, client, 5000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Já existe uma cobrança");

        // ponto central deste teste: o gateway de pagamento NUNCA deve ser
        // chamado quando já existe uma cobrança para o mesmo agendamento
        verify(abacatePayClient, never()).createBilling(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createCustomer_deveMontarRequestComDadosDoUsuario() {
        when(abacatePayClient.createCustomer(any())).thenReturn(null);

        paymentService.createCustomer(client);

        verify(abacatePayClient).createCustomer(argThatNameEmailMatch());
    }

    private agendo.app.server.modules.payment.dto.request.CreateCustomerRequest argThatNameEmailMatch() {
        return org.mockito.ArgumentMatchers.argThat(req ->
                req.getName().equals("Ana Cliente")
                        && req.getEmail().equals("ana@teste.com")
                        && req.getTaxId().equals("123.456.789-00"));
    }

    private BillingResponse stubBillingResponse(String id, String url) {
        BillingResponse response = new BillingResponse();
        BillingResponse.BillingData data = new BillingResponse.BillingData();
        data.setId(id);
        data.setUrl(url);
        data.setStatus("PENDING");
        response.setData(data);
        return response;
    }
}
