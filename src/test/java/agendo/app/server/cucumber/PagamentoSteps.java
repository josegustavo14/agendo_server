package agendo.app.server.cucumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import agendo.app.server.modules.payment.client.AbacatePayClient;
import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.repository.PaymentRepository;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

/**
 * Step definitions do cenário de pagamento (pagamento.feature).
 *
 * O AbacatePayClient injetado aqui é o MESMO bean @MockitoBean registrado
 * em CucumberSpringConfiguration — um mock controlado por nós.
 */
public class PagamentoSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private AbacatePayClient abacatePayClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${abacatepay.webhook-secret}")
    private String webhookSecret;

    @E("o gateway de pagamento está simulado para retornar a cobrança {string} com a url {string}")
    public void gatewaySimuladoParaRetornar(String billingId, String url) {
        BillingResponse stub = new BillingResponse();
        BillingResponse.BillingData data = new BillingResponse.BillingData();
        data.setId(billingId);
        data.setUrl(url);
        data.setStatus("PENDING");
        stub.setData(data);

        when(abacatePayClient.createBilling(any())).thenReturn(stub);

        context.setBillingId(billingId);
        context.setPaymentUrl(url);
    }

    @Quando("o cliente solicita o pagamento PIX do agendamento no valor de {int} centavos")
    public void clienteSolicitaOPagamentoPix(int priceInCents) {
        HttpTestResponse response = context.getHttp().post(
                "/payments/billing/appointment/" + context.getAppointmentId()
                        + "?priceInCents=" + priceInCents
                        + "&userId=" + context.getClientId(),
                null, context.getClientToken());

        context.setLastResponse(response);
    }

    @Então("a cobrança deve ser criada com sucesso")
    public void cobrancaDeveSerCriadaComSucesso() {
        assertThat(context.getLastResponse().status()).isEqualTo(200);
    }

    @E("a url de pagamento retornada deve ser {string}")
    public void urlDePagamentoRetornadaDeveSer(String urlEsperada) {
        assertThat(context.getLastResponse().path("data.url")).isEqualTo(urlEsperada);
    }

    @Quando("a AbacatePay notifica que a cobrança {string} foi paga")
    public void abacatePayNotificaQueACobrancaFoiPaga(String billingId) {
        String payload = """
                {
                  "id": "log_teste",
                  "event": "billing.paid",
                  "devMode": true,
                  "data": {
                    "billing": {
                      "id": "%s",
                      "status": "PAID",
                      "amount": 40000,
                      "paidAmount": 40000
                    }
                  }
                }
                """.formatted(billingId);

        HttpTestResponse response = context.getHttp()
                .postWithHeader("/webhooks/abacatepay", payload, "X-Webhook-Secret", webhookSecret);

        context.setLastResponse(response);
    }

    @Então("o status do pagamento deve ser atualizado para {string}")
    public void statusDoPagamentoDeveSerAtualizadoPara(String statusEsperado) {
        assertThat(context.getLastResponse().status()).isEqualTo(200);

        var payment = paymentRepository.findByAbacatePayBillingId(context.getBillingId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(statusEsperado);
    }
}