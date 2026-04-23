package agendo.app.server.modules.payment.controller;

import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.payment.dto.response.BillingListResponse;
import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.dto.response.CustomerListResponse;
import agendo.app.server.modules.payment.dto.response.CustomerResponse;
import agendo.app.server.modules.payment.service.PaymentService;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    /**
     * cria uma cobrança PIX para um agendamento.
     * retorna a URL de pagamento da AbacatePay.
     * POST /payments/billing/appointment/{appointmentId}?priceInCents=5000
     */
    @PostMapping("/billing/appointment/{appointmentId}")
    public ResponseEntity<BillingResponse> createBillingForAppointment(
            @PathVariable Long appointmentId,
            @RequestParam int priceInCents,
            @RequestParam Long userId
    ) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        BillingResponse response = paymentService.createBillingForAppointment(appointment, user, priceInCents);
        return ResponseEntity.ok(response);
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

