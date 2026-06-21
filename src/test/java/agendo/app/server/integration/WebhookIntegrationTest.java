package agendo.app.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;

/**
 * Testes de integração do endpoint de webhook (/webhooks/abacatepay).
 *
 * Estes testes só funcionam porque o SecurityConfig libera explicitamente
 * POST /webhooks/** com permitAll() — a autenticação dessa rota é feita pelo
 * próprio WebhookController via header X-Webhook-Secret, e não pelo filtro
 * JWT (a AbacatePay não tem como enviar um Bearer token nosso).
 */
class WebhookIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${abacatepay.webhook-secret}")
    private String webhookSecret;

    private record UserHandle(Long id, String token) {}

    private UserHandle createUser(String name, String role) {
        var user = persistUser(name, role);
        return new UserHandle(user.getId(), user.getToken());
    }

    private Long createAppointmentWithBilling(String billingId, String billingUrl) {
        UserHandle professional = createUser("Profissional Webhook", "PROFESSIONAL");
        UserHandle client = createUser("Cliente Webhook", "CLIENT");

        String serviceBody = """
                {"name":"Serviço Webhook","description":"teste","price":250.00}
                """;
        HttpTestResponse serviceResponse = http.post("/service-types", serviceBody, professional.token());
        Long serviceTypeId = serviceResponse.pathAsLong("id");

        String appointmentBody = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(1));
        HttpTestResponse appointmentResponse = http.post("/appointments", appointmentBody, client.token());
        Long appointmentId = appointmentResponse.pathAsLong("id");

        BillingResponse stub = new BillingResponse();
        BillingResponse.BillingData data = new BillingResponse.BillingData();
        data.setId(billingId);
        data.setUrl(billingUrl);
        data.setStatus("PENDING");
        stub.setData(data);
        when(abacatePayClient.createBilling(any())).thenReturn(stub);

        HttpTestResponse billing = http.post(
                "/payments/billing/appointment/" + appointmentId + "?priceInCents=25000&userId=" + client.id(),
                null, client.token());
        assertThat(billing.status()).isEqualTo(200);

        return appointmentId;
    }

    private String webhookPayload(String event, String billingId) {
        return """
                {
                  "id": "log_teste",
                  "event": "%s",
                  "devMode": true,
                  "data": {
                    "billing": {
                      "id": "%s",
                      "status": "PAID",
                      "amount": 25000,
                      "paidAmount": 25000
                    }
                  }
                }
                """.formatted(event, billingId);
    }

    @Test
    void webhookBillingPaid_comSecretCorreto_atualizaStatusParaPaid() {
        createAppointmentWithBilling("bill_webhook_1", "https://pay.abacatepay.com/bill_webhook_1");

        HttpTestResponse resp = http.postWithHeader(
                "/webhooks/abacatepay", webhookPayload("billing.paid", "bill_webhook_1"),
                "X-Webhook-Secret", webhookSecret);
        assertThat(resp.status()).isEqualTo(200);

        var payment = paymentRepository.findByAbacatePayBillingId("bill_webhook_1").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo("PAID");
    }

    @Test
    void webhook_comSecretInvalido_retorna401ENaoAlteraOPagamento() {
        createAppointmentWithBilling("bill_webhook_2", "https://pay.abacatepay.com/bill_webhook_2");

        HttpTestResponse resp = http.postWithHeader(
                "/webhooks/abacatepay", webhookPayload("billing.paid", "bill_webhook_2"),
                "X-Webhook-Secret", "secret-errado");
        assertThat(resp.status()).isEqualTo(401);

        var payment = paymentRepository.findByAbacatePayBillingId("bill_webhook_2").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void webhook_paraCobrancaDesconhecida_retorna200SemQuebrar() {
        // nenhuma cobrança com esse ID existe — deve ser idempotente (loga e ignora), sem 500
        HttpTestResponse resp = http.postWithHeader(
                "/webhooks/abacatepay", webhookPayload("billing.paid", "bill_que_nao_existe"),
                "X-Webhook-Secret", webhookSecret);
        assertThat(resp.status()).isEqualTo(200);
    }

    @Test
    void webhookBillingFailed_atualizaStatusParaFailed() {
        createAppointmentWithBilling("bill_webhook_3", "https://pay.abacatepay.com/bill_webhook_3");

        HttpTestResponse resp = http.postWithHeader(
                "/webhooks/abacatepay", webhookPayload("billing.failed", "bill_webhook_3"),
                "X-Webhook-Secret", webhookSecret);
        assertThat(resp.status()).isEqualTo(200);

        var payment = paymentRepository.findByAbacatePayBillingId("bill_webhook_3").orElseThrow();
        assertThat(payment.getStatus()).isEqualTo("FAILED");
    }
}