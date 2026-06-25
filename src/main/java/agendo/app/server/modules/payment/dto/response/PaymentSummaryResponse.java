package agendo.app.server.modules.payment.dto.response;

/**
 * Resposta enxuta de uma cobrança PIX já persistida localmente.
 * Usada por GET /payments/by-appointment/{id} — não expõe dados internos
 * do gateway, apenas o que o cliente precisa para pagar e ver o status.
 */
public record PaymentSummaryResponse(
        Long id,
        String billingId,
        String paymentUrl,
        String status,
        Integer amountInCents,
        Long appointmentId
) {}
