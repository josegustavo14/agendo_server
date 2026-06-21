package agendo.app.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;

/**
 * Testes de integração do módulo de pagamento.
 *
 * O AbacatePayClient (herdado de IntegrationTestBase) é um @MockitoBean: a
 * aplicação sobe de verdade, o HTTP é real, o banco é real (H2) — mas a
 * chamada para a AbacatePay é interceptada e controlada por nós.
 *
 * Novo fluxo: a cobrança PIX é gerada AUTOMATICAMENTE quando o profissional
 * aprova o agendamento (PaymentOnApprovalListener, AFTER_COMMIT). O endpoint
 * POST /payments/billing/appointment/{id} passa a ser apenas uma
 * RE-TENTATIVA manual (usa o usuário autenticado, sem params na URL).
 */
class PaymentIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PaymentRepository paymentRepository;

    private record UserHandle(Long id, String token) {}

    private UserHandle createUser(String name, String role) {
        var user = persistUser(name, role);
        return new UserHandle(user.getId(), user.getToken());
    }

    private Long createAppointment(UserHandle professional, UserHandle client, double price) {
        String serviceBody = """
                {"name":"Serviço Pagamento","description":"teste","price":%s}
                """.formatted(price);

        HttpTestResponse serviceResponse = http.post("/service-types", serviceBody, professional.token());
        Long serviceTypeId = serviceResponse.pathAsLong("id");

        String appointmentBody = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(1));

        HttpTestResponse appointmentResponse = http.post("/appointments", appointmentBody, client.token());
        return appointmentResponse.pathAsLong("id");
    }

    private void approve(Long appointmentId, UserHandle professional) {
        HttpTestResponse resp = http.patch("/appointments/" + appointmentId + "/approve", null, professional.token());
        assertThat(resp.status()).isEqualTo(200);
    }

    private BillingResponse stubBilling(String id, String url) {
        BillingResponse response = new BillingResponse();
        BillingResponse.BillingData data = new BillingResponse.BillingData();
        data.setId(id);
        data.setUrl(url);
        data.setStatus("PENDING");
        response.setData(data);
        return response;
    }

    @Test
    void aprovarAgendamento_geraCobrancaAutomaticamente() {
        UserHandle professional = createUser("Camila Manicure", "PROFESSIONAL");
        UserHandle client = createUser("Diego Cliente", "CLIENT");
        Long appointmentId = createAppointment(professional, client, 80.00);

        when(abacatePayClient.createBilling(any()))
                .thenReturn(stubBilling("bill_auto", "https://pay.abacatepay.com/bill_auto"));

        // aprovar dispara a cobrança via listener AFTER_COMMIT
        approve(appointmentId, professional);

        // a geração é assíncrona ao request; aguardamos o registro aparecer
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var payment = paymentRepository.findByAppointmentId(appointmentId);
            assertThat(payment).isPresent();
            assertThat(payment.get().getAbacatePayBillingId()).isEqualTo("bill_auto");
            assertThat(payment.get().getStatus()).isEqualTo("PENDING");
        });
    }

    @Test
    void retryCobranca_quandoAindaNaoExiste_retornaUrlDoGateway() {
        UserHandle professional = createUser("Bruno Jardineiro", "PROFESSIONAL");
        UserHandle client = createUser("Elaine Cliente", "CLIENT");
        Long appointmentId = createAppointment(professional, client, 120.00);

        when(abacatePayClient.createBilling(any()))
                .thenReturn(stubBilling("bill_retry", "https://pay.abacatepay.com/bill_retry"));

        // sem aprovar: dispara manualmente a cobrança (re-tentativa), autenticado como cliente
        HttpTestResponse resp = http.post("/payments/billing/appointment/" + appointmentId, null, client.token());

        assertThat(resp.status()).isEqualTo(200);
        assertThat(resp.path("data.id")).isEqualTo("bill_retry");
        assertThat(resp.path("data.url")).isEqualTo("https://pay.abacatepay.com/bill_retry");
    }

    @Test
    void retryCobranca_quandoJaExiste_retorna409() {
        UserHandle professional = createUser("Paula Cabeleireira", "PROFESSIONAL");
        UserHandle client = createUser("Rui Cliente", "CLIENT");
        Long appointmentId = createAppointment(professional, client, 60.00);

        when(abacatePayClient.createBilling(any()))
                .thenReturn(stubBilling("bill_once", "https://pay.abacatepay.com/bill_once"));

        // primeira: cria
        assertThat(http.post("/payments/billing/appointment/" + appointmentId, null, client.token()).status())
                .isEqualTo(200);

        // segunda para o mesmo agendamento: já existe cobrança -> 409 Conflict
        assertThat(http.post("/payments/billing/appointment/" + appointmentId, null, client.token()).status())
                .isEqualTo(409);
    }

    @Test
    void retryCobranca_porUsuarioQueNaoEhOCliente_retorna403() {
        UserHandle professional = createUser("Sandro Pintor", "PROFESSIONAL");
        UserHandle client = createUser("Vera Cliente", "CLIENT");
        UserHandle estranho = createUser("Estranho Qualquer", "CLIENT");
        Long appointmentId = createAppointment(professional, client, 90.00);

        // um terceiro tentando gerar a cobrança do agendamento alheio
        assertThat(http.post("/payments/billing/appointment/" + appointmentId, null, estranho.token()).status())
                .isEqualTo(403);
    }
}