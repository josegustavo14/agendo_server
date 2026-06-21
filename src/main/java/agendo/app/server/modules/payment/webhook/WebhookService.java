package agendo.app.server.modules.payment.webhook;

import agendo.app.server.modules.payment.models.PaymentEntity;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository paymentRepository;

    /**
     * roteia e processa o evento recebido.
     * Toda a operação é idempotente: se o evento já foi processado,
     * apenas loga e retorna sem erro.
     */
    @Transactional
    public void handle(AbacatePayWebhookPayload payload) {

        WebhookEvent event = WebhookEvent.fromString(payload.getEvent());

        log.info("[Webhook] Evento recebido: {} | devMode: {} | billingId: {}",
                event, payload.getDevMode(),
                payload.getData() != null && payload.getData().getBilling() != null
                        ? payload.getData().getBilling().getId()
                        : "N/A");

        switch (event) {
            case BILLING_PAID     -> handleBillingPaid(payload);
            case BILLING_FAILED   -> handleBillingFailed(payload);
            case BILLING_REFUNDED -> handleBillingRefunded(payload);
            case BILLING_CREATED  -> log.info("[Webhook] Cobrança criada: {}", payload.getData().getBilling());
            case UNKNOWN          -> log.warn("[Webhook] Evento desconhecido ignorado: {}", payload.getEvent());
        }
    }

    private void handleBillingPaid(AbacatePayWebhookPayload payload) {

        String billingId = payload.getData().getBilling().getId();

        PaymentEntity payment = paymentRepository
                .findByAbacatePayBillingId(billingId)
                .orElse(null);

        if (payment == null) {
            log.warn("[Webhook] billing.paid recebido para cobrança desconhecida: {}", billingId);
            return;
        }

        if ("PAID".equals(payment.getStatus())) {
            log.info("[Webhook] billing.paid já processado anteriormente para: {}", billingId);
            return; // idempotência
        }

        payment.setStatus("PAID");
        paymentRepository.save(payment);

        log.info("[Webhook] Pagamento confirmado: billingId={} | appointmentId={}",
                billingId,
                payment.getAppointment() != null ? payment.getAppointment().getId() : "N/A");

        // aqui podemos disparar notificações, e-mails, atualizar o status do agendamento, etc.
        // ex: appointmentService.markAsPaid(payment.getAppointment().getId());
    }

    private void handleBillingFailed(AbacatePayWebhookPayload payload) {

        String billingId = payload.getData().getBilling().getId();

        paymentRepository.findByAbacatePayBillingId(billingId).ifPresentOrElse(
                payment -> {
                    payment.setStatus("FAILED");
                    paymentRepository.save(payment);
                    log.info("[Webhook] Pagamento falhou: {}", billingId);
                },
                () -> log.warn("[Webhook] billing.failed para cobrança desconhecida: {}", billingId)
        );
    }

    private void handleBillingRefunded(AbacatePayWebhookPayload payload) {

        String billingId = payload.getData().getBilling().getId();

        paymentRepository.findByAbacatePayBillingId(billingId).ifPresentOrElse(
                payment -> {
                    payment.setStatus("REFUNDED");
                    paymentRepository.save(payment);
                    log.info("[Webhook] Pagamento reembolsado: {}", billingId);
                },
                () -> log.warn("[Webhook] billing.refunded para cobrança desconhecida: {}", billingId)
        );
    }
}