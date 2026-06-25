package agendo.app.server.modules.payment.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import agendo.app.server.modules.appointment.repository.AppointmentRepository;
import agendo.app.server.modules.payment.models.PaymentEntity;
import agendo.app.server.modules.payment.repository.PaymentRepository;

/**
 * Testes unitários do WebhookService — a peça que processa as notificações
 * enviadas pela AbacatePay quando o status de uma cobrança muda.
 *
 * O ponto mais importante destes testes é a idempotência: a AbacatePay pode
 * entregar o mesmo evento mais de uma vez, e processar o mesmo "billing.paid"
 * duas vezes não deve gerar efeitos colaterais duplicados.
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private WebhookService webhookService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        webhookService = new WebhookService(paymentRepository, appointmentRepository);
    }

    @Test
    void handle_billingPaid_atualizaStatusParaPaid() {
        PaymentEntity payment = PaymentEntity.builder()
                .abacatePayBillingId("bill_1").status("PENDING").build();

        when(paymentRepository.findByAbacatePayBillingId("bill_1")).thenReturn(Optional.of(payment));

        webhookService.handle(payload("billing.paid", "bill_1"));

        assertThat(payment.getStatus()).isEqualTo("PAID");
        verify(paymentRepository).save(payment);
    }

    @Test
    void handle_billingPaid_idempotente_naoSalvaDeNovoSeJaEstavaPago() {
        PaymentEntity payment = PaymentEntity.builder()
                .abacatePayBillingId("bill_2").status("PAID") // já processado antes
                .build();

        when(paymentRepository.findByAbacatePayBillingId("bill_2")).thenReturn(Optional.of(payment));

        webhookService.handle(payload("billing.paid", "bill_2"));

        // o evento é reprocessado sem erro, mas não deve persistir de novo
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handle_billingPaid_cobrancaDesconhecida_naoLancaExcecao() {
        when(paymentRepository.findByAbacatePayBillingId("bill_inexistente")).thenReturn(Optional.empty());

        webhookService.handle(payload("billing.paid", "bill_inexistente"));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handle_billingFailed_atualizaStatusParaFailed() {
        PaymentEntity payment = PaymentEntity.builder()
                .abacatePayBillingId("bill_3").status("PENDING").build();

        when(paymentRepository.findByAbacatePayBillingId("bill_3")).thenReturn(Optional.of(payment));

        webhookService.handle(payload("billing.failed", "bill_3"));

        assertThat(payment.getStatus()).isEqualTo("FAILED");
        verify(paymentRepository).save(payment);
    }

    @Test
    void handle_billingRefunded_atualizaStatusParaRefunded() {
        PaymentEntity payment = PaymentEntity.builder()
                .abacatePayBillingId("bill_4").status("PAID").build();

        when(paymentRepository.findByAbacatePayBillingId("bill_4")).thenReturn(Optional.of(payment));

        webhookService.handle(payload("billing.refunded", "bill_4"));

        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void handle_billingCreated_apenasLogaSemAlterarOPagamento() {
        // o evento billing.created não dispara nenhuma atualização de status,
        // apenas um log informativo — usamos um payload com billing não-nulo
        // porque o código atual acessa payload.getData().getBilling() direto,
        // sem null-check, nesse branch específico (ver handle() acima)
        webhookService.handle(payload("billing.created", "bill_5"));

        verify(paymentRepository, never()).findByAbacatePayBillingId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handle_eventoDesconhecido_naoInterageComORepositorio() {
        AbacatePayWebhookPayload payload = new AbacatePayWebhookPayload();
        payload.setEvent("subscription.renewed"); // evento não mapeado
        payload.setData(null);

        webhookService.handle(payload);

        verify(paymentRepository, never()).findByAbacatePayBillingId(any());
        verify(paymentRepository, never()).save(any());
    }

    private AbacatePayWebhookPayload payload(String event, String billingId) {
        AbacatePayWebhookPayload.BillingData billing = new AbacatePayWebhookPayload.BillingData();
        billing.setId(billingId);

        AbacatePayWebhookPayload.DataWrapper data = new AbacatePayWebhookPayload.DataWrapper();
        data.setBilling(billing);

        AbacatePayWebhookPayload payload = new AbacatePayWebhookPayload();
        payload.setEvent(event);
        payload.setDevMode(true);
        payload.setData(data);
        return payload;
    }
}
