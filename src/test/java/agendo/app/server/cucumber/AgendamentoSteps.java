package agendo.app.server.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

/**
 * Step definitions do cenário de agendamento (agendamento.feature).
 * Também é reutilizado pelo pagamento.feature para montar o cenário base
 * (profissional + serviço + cliente + agendamento) antes de testar a cobrança.
 *
 * O profissional e o cliente são persistidos DIRETO no banco via
 * TestUserFactory — essas steps não testam o cadastro em si, só precisam de
 * usuários existentes e autenticados para montar o cenário.
 */
public class AgendamentoSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private TestUserFactory userFactory;

    @Dado("que existe um profissional {string} com email {string}")
    public void existeUmProfissional(String nome, String email) {
        UserEntity user = userFactory.create(nome, comSufixoUnico(email), "PROFESSIONAL");
        context.setProfessionalId(user.getId());
        context.setProfessionalToken(user.getToken());
    }

    @E("o profissional cadastrou o serviço {string} pelo valor de {double}")
    public void profissionalCadastrouOServico(String nome, double preco) {
        String body = """
                {"name":"%s","description":"Serviço de teste","price":%s}
                """.formatted(nome, preco);

        HttpTestResponse response = context.getHttp().post("/service-types", body, context.getProfessionalToken());
        context.setServiceTypeId(response.pathAsLong("id"));
    }

    @E("existe um cliente {string} com email {string}")
    public void existeUmCliente(String nome, String email) {
        UserEntity user = userFactory.create(nome, comSufixoUnico(email), "CLIENT");
        context.setClientId(user.getId());
        context.setClientToken(user.getToken());
    }

    @Quando("o cliente cria um agendamento com o profissional para o serviço {string}")
    public void clienteCriaUmAgendamento(String nomeServico) {
        String body = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(
                context.getProfessionalId(),
                context.getClientId(),
                context.getServiceTypeId(),
                LocalDateTime.now().plusDays(3));

        HttpTestResponse response = context.getHttp().post("/appointments", body, context.getClientToken());
        context.setLastResponse(response);

        if (response.status() == 201) {
            context.setAppointmentId(response.pathAsLong("id"));
        }
    }

    @Quando("o profissional aprova o agendamento")
    public void profissionalAprovaOAgendamento() {
        HttpTestResponse response = context.getHttp()
                .patch("/appointments/" + context.getAppointmentId() + "/approve", null, context.getProfessionalToken());
        context.setLastResponse(response);
    }

    @E("o cliente tenta aprovar o próprio agendamento")
    public void clienteTentaAprovarOProprioAgendamento() {
        HttpTestResponse response = context.getHttp()
                .patch("/appointments/" + context.getAppointmentId() + "/approve", null, context.getClientToken());
        context.setLastResponse(response);
    }

    @Então("o agendamento deve ser criado com status {string}")
    public void agendamentoDeveSerCriadoComStatus(String statusEsperado) {
        assertThat(context.getLastResponse().status()).isEqualTo(201);
        assertThat(context.getLastResponse().path("status")).isEqualTo(statusEsperado);
    }

    @E("o valor total do agendamento deve ser {double}")
    public void valorTotalDoAgendamentoDeveSer(double valorEsperado) {
        assertThat(context.getLastResponse().pathAsDouble("totalAmount")).isEqualTo(valorEsperado);
    }

    @Então("o status do agendamento deve ser {string}")
    public void statusDoAgendamentoDeveSer(String statusEsperado) {
        assertThat(context.getLastResponse().path("status")).isEqualTo(statusEsperado);
    }

    @Então("a aprovação deve ser rejeitada com status {int}")
    public void aprovacaoDeveSerRejeitadaComStatus(int statusEsperado) {
        assertThat(context.getLastResponse().status()).isEqualTo(statusEsperado);
    }

    // helpers

    private String comSufixoUnico(String email) {
        String[] partes = email.split("@");
        return partes[0] + "-" + UUID.randomUUID().toString().substring(0, 6) + "@" + partes[1];
    }
}