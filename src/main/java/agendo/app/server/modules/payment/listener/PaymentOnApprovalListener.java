package agendo.app.server.modules.payment.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import agendo.app.server.modules.appointment.events.AppointmentApprovedEvent;
import agendo.app.server.modules.appointment.models.AppointmentEntity;
import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gera a cobrança PIX automaticamente quando um agendamento é aprovado.
 *
 * Por que AFTER_COMMIT (e não dentro do approve)?
 * A criação da cobrança faz uma chamada HTTP externa à AbacatePay. Mantê-la
 * fora da transação do approve garante que:
 *  - a aprovação é persistida independentemente do gateway estar disponível;
 *  - uma falha da AbacatePay não desfaz a aprovação do profissional;
 *  - não há chamada de rede segurando locks de banco abertos.
 *
 * Quem paga é o CLIENTE do agendamento; o valor vem do totalAmount do próprio
 * agendamento — nada de userId/preço vindos de fora, evitando manipulação.
 *
 * É idempotente: o PaymentService já bloqueia cobrança duplicada para o mesmo
 * agendamento (findByAppointmentId), então reentregas do evento não geram
 * cobranças repetidas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOnApprovalListener {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentApproved(AppointmentApprovedEvent event) {
        Long appointmentId = event.appointmentId();

        if (paymentRepository.findByAppointmentId(appointmentId).isPresent()) {
            log.info("[Payment] Agendamento {} já possui cobrança; ignorando evento de aprovação.", appointmentId);
            return;
        }

        AppointmentEntity appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            log.warn("[Payment] Agendamento {} não encontrado ao gerar cobrança pós-aprovação.", appointmentId);
            return;
        }

        int priceInCents = toCents(appointment.getTotalAmount());

        try {
            paymentService.createBillingForAppointment(
                    appointment,
                    appointment.getClient(),
                    priceInCents);
            log.info("[Payment] Cobrança gerada automaticamente após aprovação do agendamento {}.", appointmentId);
        } catch (Exception ex) {
            log.error("[Payment] Falha ao gerar cobrança para o agendamento {}: {}",
                    appointmentId, ex.getMessage(), ex);
        }
    }

    private int toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}