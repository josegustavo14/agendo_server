package agendo.app.server.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

/**
 * Step definitions do cenário de cadastro e login (cadastro_login.feature).
 *
 * IMPORTANTE: POST /users exige autenticação. Nos cenários de CADASTRO
 * criamos um usuário "chamador" direto no banco via TestUserFactory só para
 * fornecer o token usado na chamada. Nos cenários de LOGIN, o usuário já é
 * persistido direto no banco, porque o que importa é testar /users/login.
 */
public class CadastroLoginSteps {

    @Autowired
    private TestContext context;

    @Autowired
    private TestUserFactory userFactory;

    @Quando("eu cadastro um profissional chamado {string} com email {string}")
    public void euCadastroUmProfissional(String nome, String email) {
        cadastrarComSenha(nome, comSufixoUnico(email), "senha123", "PROFESSIONAL");
    }

    @Quando("eu cadastro um cliente chamado {string} com email {string}")
    public void euCadastroUmCliente(String nome, String email) {
        cadastrarComSenha(nome, comSufixoUnico(email), "senha123", "CLIENT");
    }

    @Dado("que um usuário {string} com email {string} e senha {string} está cadastrado")
    public void usuarioEstaCadastrado(String nome, String email, String senha) {
        String emailUnico = comSufixoUnico(email);
        // persistência direta: só prepara o cenário para o teste de LOGIN
        userFactory.create(nome, emailUnico, "PROFESSIONAL", senha);
        context.setLastEmail(emailUnico);
    }

    @Quando("eu faço login com email {string} e senha {string}")
    public void euFacoLogin(String email, String senha) {
        String emailReal = context.getLastEmail() != null ? context.getLastEmail() : email;

        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(emailReal, senha);

        HttpTestResponse response = context.getHttp().post("/users/login", body, null);
        context.setLastResponse(response);
    }

    @Então("o cadastro deve ser aceito com sucesso")
    public void cadastroAceitoComSucesso() {
        assertThat(context.getLastResponse().status()).isEqualTo(201);
    }

    @Então("o login deve ser aceito com sucesso")
    public void loginAceitoComSucesso() {
        assertThat(context.getLastResponse().status()).isEqualTo(200);
    }

    @Então("o login deve ser rejeitado com status {int}")
    public void loginRejeitadoComStatus(int statusEsperado) {
        assertThat(context.getLastResponse().status()).isEqualTo(statusEsperado);
    }

    @E("o usuário cadastrado deve ter a role {string}")
    public void usuarioCadastradoTemRole(String roleEsperada) {
        assertThat(context.getLastResponse().path("role")).isEqualTo(roleEsperada);
    }

    @E("um token de autenticação deve ser retornado")
    public void tokenDeAutenticacaoRetornado() {
        assertThat(context.getLastResponse().path("token")).isNotBlank();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void cadastrarComSenha(String nome, String email, String senha, String role) {
        // chamador autenticado só para ter um token válido — o teste valida
        // o cadastro do usuário "nome"/"email", não esse chamador
        UserEntity caller = userFactory.create(
                "Chamador Cucumber", comSufixoUnico("chamador-cucumber@teste.com"), "PROFESSIONAL");

        String body = """
                {"name":"%s","email":"%s","phone":"11999990000","role":"%s","password":"%s"}
                """.formatted(nome, email, role, senha);

        HttpTestResponse response = context.getHttp().post("/users", body, caller.getToken());
        context.setLastResponse(response);
    }

    private String comSufixoUnico(String email) {
        String[] partes = email.split("@");
        return partes[0] + "-" + UUID.randomUUID().toString().substring(0, 6) + "@" + partes[1];
    }
}