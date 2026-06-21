package agendo.app.server.modules.appointment.events;

/**
 * Evento publicado quando um agendamento é aprovado pelo profissional.
 * É consumido por um listener AFTER_COMMIT (PaymentOnApprovalListener), que
 * dispara a geração da cobrança PIX. Carrega apenas o id do agendamento — o
 * listener recarrega a entidade fresca do banco, evitando trafegar entidades
 * gerenciadas entre transações.
 */
public record AppointmentApprovedEvent(Long appointmentId) {
}