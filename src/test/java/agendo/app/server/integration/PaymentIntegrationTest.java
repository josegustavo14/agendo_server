package agendo.app.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;

/**
 * Testes de integração do módulo de pagamento.
 *
 * O AbacatePayClient (herdado de IntegrationTestBase) é um @MockitoBean: a
 * aplicação sobe de verdade, o HTTP é real, o banco é real (H2) — mas a
 * chamada para a AbacatePay é interceptada e controlada por nós.
 */
class PaymentIntegrationTest extends IntegrationTestBase {

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
    void criarCobranca_retornaUrlDePagamentoDoGatewayMockado() {
        UserHandle professional = createUser("Camila Manicure", "PROFESSIONAL");
        UserHandle client = createUser("Diego Cliente", "CLIENT");
        Long appointmentId = createAppointment(professional, client, 80.00);

        when(abacatePayClient.createBilling(any()))
                .thenReturn(stubBilling("bill_int_test", "https://pay.abacatepay.com/bill_int_test"));

        HttpTestResponse resp = http.post(
                "/payments/billing/appointment/" + appointmentId + "?priceInCents=8000&userId=" + client.id(),
                null, client.token());

        assertThat(resp.status()).isEqualTo(200);
        assertThat(resp.path("data.id")).isEqualTo("bill_int_test");
        assertThat(resp.path("data.url")).isEqualTo("https://pay.abacatepay.com/bill_int_test");
    }

    @Test
    void criarCobrancaDuplicadaParaOMesmoAgendamento_falha() {
        UserHandle professional = createUser("Bruno Jardineiro", "PROFESSIONAL");
        UserHandle client = createUser("Elaine Cliente", "CLIENT");
        Long appointmentId = createAppointment(professional, client, 120.00);

        when(abacatePayClient.createBilling(any()))
                .thenReturn(stubBilling("bill_dup", "https://pay.abacatepay.com/bill_dup"));

        String url = "/payments/billing/appointment/" + appointmentId + "?priceInCents=12000&userId=" + client.id();

        // primeira cobrança: sucesso
        assertThat(http.post(url, null, client.token()).status()).isEqualTo(200);

        // segunda cobrança para o mesmo agendamento: o PaymentService lança
        // IllegalStateException, sem handler dedicado, então o Spring devolve 500.
        assertThat(http.post(url, null, client.token()).status()).isEqualTo(500);
    }
}