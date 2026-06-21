package agendo.app.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import agendo.app.server.support.HttpTestClient.HttpTestResponse;

/**
 * Testes de integração do fluxo completo de agendamento:
 * cadastro de profissional e cliente -> criação de tipo de serviço ->
 * criação de agendamento -> aprovação -> conclusão.
 */
class AppointmentIntegrationTest extends IntegrationTestBase {

    private record UserHandle(Long id, String token) {}

    private UserHandle createUser(String name, String role) {
        var user = persistUser(name, role);
        return new UserHandle(user.getId(), user.getToken());
    }

    private Long createServiceType(String professionalToken, String name, double price) {
        String body = """
                {"name":"%s","description":"Serviço de teste","price":%s}
                """.formatted(name, price);

        HttpTestResponse resp = http.post("/service-types", body, professionalToken);
        return resp.pathAsLong("id");
    }

    @Test
    void criarAgendamento_calculaValorTotalEComecaComoPending() {
        UserHandle professional = createUser("João Pintor", "PROFESSIONAL");
        UserHandle client = createUser("Ana Cliente", "CLIENT");
        Long serviceTypeId = createServiceType(professional.token(), "Pintura de Parede", 200.00);

        String body = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(2));

        HttpTestResponse resp = http.post("/appointments", body, client.token());

        assertThat(resp.status()).isEqualTo(201);
        assertThat(resp.path("status")).isEqualTo("PENDING");
        assertThat(resp.pathAsDouble("totalAmount")).isEqualTo(200.00);
    }

    @Test
    void criarAgendamento_usuarioQueNaoParticipaRecebeForbidden() {
        UserHandle professional = createUser("Lucas Encanador", "PROFESSIONAL");
        UserHandle client = createUser("Maria Cliente", "CLIENT");
        UserHandle estranho = createUser("Pedro Estranho", "CLIENT");
        Long serviceTypeId = createServiceType(professional.token(), "Conserto", 100.00);

        String body = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(1));

        // token de um terceiro que não é nem o profissional nem o cliente
        assertThat(http.post("/appointments", body, estranho.token()).status()).isEqualTo(403);
    }

    @Test
    void fluxoCompleto_aprovarEConcluirAgendamento() {
        UserHandle professional = createUser("Rita Designer", "PROFESSIONAL");
        UserHandle client = createUser("Felipe Cliente", "CLIENT");
        Long serviceTypeId = createServiceType(professional.token(), "Identidade Visual", 500.00);

        String createBody = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(3));

        HttpTestResponse created = http.post("/appointments", createBody, client.token());
        Long appointmentId = created.pathAsLong("id");

        // profissional aprova
        HttpTestResponse approved = http.patch("/appointments/" + appointmentId + "/approve", null, professional.token());
        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.path("status")).isEqualTo("APPROVED");

        // profissional conclui
        HttpTestResponse completed = http.patch("/appointments/" + appointmentId + "/complete", null, professional.token());
        assertThat(completed.status()).isEqualTo(200);
        assertThat(completed.path("status")).isEqualTo("COMPLETED");
    }

    @Test
    void clienteNaoPodeAprovarOProprioAgendamento() {
        UserHandle professional = createUser("Sergio Fotografo", "PROFESSIONAL");
        UserHandle client = createUser("Tatiana Cliente", "CLIENT");
        Long serviceTypeId = createServiceType(professional.token(), "Sessão de Fotos", 400.00);

        String createBody = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(1));

        HttpTestResponse created = http.post("/appointments", createBody, client.token());
        Long appointmentId = created.pathAsLong("id");

        assertThat(http.patch("/appointments/" + appointmentId + "/approve", null, client.token()).status())
                .isEqualTo(403);
    }

    @Test
    void listarAgendamentosAtivos_retornaApenasOsCriados() {
        UserHandle professional = createUser("Gabriel Marceneiro", "PROFESSIONAL");
        UserHandle client = createUser("Larissa Cliente", "CLIENT");
        Long serviceTypeId = createServiceType(professional.token(), "Móvel planejado", 1500.00);

        String createBody = """
                {"professionalId":%d,"clientId":%d,"serviceTypeIds":[%d],"scheduleDate":"%s"}
                """.formatted(professional.id(), client.id(), serviceTypeId, LocalDateTime.now().plusDays(5));

        http.post("/appointments", createBody, client.token());

        HttpTestResponse list = http.get("/appointments/active", client.token());
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.rootArraySize()).isGreaterThanOrEqualTo(1);
    }
}