package agendo.app.server.modules.payment.controller;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.payment.dto.response.BillingListResponse;
import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.dto.response.CustomerListResponse;
import agendo.app.server.modules.payment.dto.response.CustomerResponse;
import agendo.app.server.modules.payment.dto.response.PaymentSummaryResponse;
import agendo.app.server.modules.payment.models.PaymentEntity;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.modules.payment.service.PaymentService;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Re-tentativa MANUAL da geração de cobrança PIX de um agendamento.
     *
     * No fluxo normal, a cobrança é criada automaticamente quando o
     * profissional aprova o agendamento (PaymentOnApprovalListener). Este
     * endpoint serve apenas para RE-TENTAR caso aquela geração automática
     * tenha falhado (ex: AbacatePay indisponível no momento da aprovação).
     *
     * Diferenças em relação à versão antiga (insegura):
     *  - O usuário vem do TOKEN (@AuthenticationPrincipal), não de um
     *    parâmetro userId manipulável na URL.
     *  - Só o CLIENTE do próprio agendamento pode disparar (senão 403).
     *  - O valor vem do totalAmount do agendamento, não de um priceInCents
     *    arbitrário na query string.
     *  - Se já existir cobrança, o PaymentService lança IllegalStateException;
     *    aqui traduzimos para 409 Conflict (em vez de 500).
     *
     * POST /payments/billing/appointment/{appointmentId}
     */
    @PostMapping("/billing/appointment/{appointmentId}")
    public ResponseEntity<BillingResponse> retryBillingForAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserEntity user
    ) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado"));

        // só o cliente do agendamento pode gerar/retentar a própria cobrança
        if (!appointment.getClient().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas o cliente do agendamento pode gerar a cobrança");
        }

        int priceInCents = toCents(appointment.getTotalAmount());

        try {
            BillingResponse response =
                    paymentService.createBillingForAppointment(appointment, user, priceInCents);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException alreadyExists) {
            // idempotência do PaymentService: já há cobrança para o agendamento
            throw new ResponseStatusException(HttpStatus.CONFLICT, alreadyExists.getMessage());
        }
    }

    private int toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * Lê a cobrança PIX já persistida para um agendamento.
     *
     * Como a cobrança é criada AUTOMATICAMENTE pelo PaymentOnApprovalListener
     * quando o profissional aprova o agendamento, o frontend chama este GET
     * primeiro para obter a URL/status existente. Só recorre ao POST acima
     * se este retornar 404 (cobrança ainda não foi gerada).
     *
     * Apenas o cliente do agendamento pode consultar.
     *
     * GET /payments/by-appointment/{appointmentId}
     */
    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<PaymentSummaryResponse> getByAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal UserEntity user) {

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado"));

        if (!appointment.getClient().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas o cliente do agendamento pode consultar a cobrança");
        }

        PaymentEntity payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma cobrança encontrada para o agendamento " + appointmentId));

        return ResponseEntity.ok(new PaymentSummaryResponse(
                payment.getId(),
                payment.getAbacatePayBillingId(),
                payment.getPaymentUrl(),
                payment.getStatus(),
                payment.getAmountInCents(),
                payment.getAppointment().getId()
        ));
    }

    /** lista todas as cobranças da sua conta na AbacatePay. */
    @GetMapping("/billing")
    public ResponseEntity<BillingListResponse> listBillings() {
        return ResponseEntity.ok(paymentService.listBillings());
    }

    /**
     * registra um cliente na AbacatePay manualmente.
     * POST /payments/customer?userId=1
     */
    @PostMapping("/customer")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestParam Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return ResponseEntity.ok(paymentService.createCustomer(user));
    }

    /** lista todos os clientes cadastrados na AbacatePay. */
    @GetMapping("/customer")
    public ResponseEntity<CustomerListResponse> listCustomers() {
        return ResponseEntity.ok(paymentService.listCustomers());
    }
}