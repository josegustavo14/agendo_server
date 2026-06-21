package agendo.app.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;

/**
 * Testes de integração do fluxo de cadastro e autenticação.
 *
 * IMPORTANTE: POST /users exige autenticação. Os testes que validam o
 * próprio endpoint de cadastro usam um usuário "chamador" já persistido
 * (via persistUser) para fornecer o token. Os testes de login persistem o
 * usuário direto no banco, pois o que se testa é /users/login.
 */
class UserIntegrationTest extends IntegrationTestBase {

    @Test
    void criarUsuarioProfissional_retorna201ComTokenEPerfilProfissional() {
        UserEntity caller = persistUser("Chamador Autenticado", "PROFESSIONAL");

        String body = """
                {"name":"João Eletricista","email":"%s","phone":"11999990000","role":"PROFESSIONAL","password":"senha123","bio":"Eletricista há 10 anos"}
                """.formatted(uniqueEmail("joao"));

        HttpTestResponse resp = http.post("/users", body, caller.getToken());

        assertThat(resp.status()).isEqualTo(201);
        assertThat(resp.path("name")).isEqualTo("João Eletricista");
        assertThat(resp.path("role")).isEqualTo("PROFESSIONAL");
        assertThat(resp.path("token")).isNotNull();
    }

    @Test
    void criarUsuarioCliente_retorna201ComPerfilDeCliente() {
        UserEntity caller = persistUser("Chamador Autenticado 2", "CLIENT");

        String body = """
                {"name":"Maria Cliente","email":"%s","phone":"11988880000","role":"CLIENT","password":"senha123","taxId":"123.456.789-00"}
                """.formatted(uniqueEmail("maria"));

        HttpTestResponse resp = http.post("/users", body, caller.getToken());

        assertThat(resp.status()).isEqualTo(201);
        assertThat(resp.path("role")).isEqualTo("CLIENT");
        assertThat(resp.path("clientProfile.taxId")).isEqualTo("123.456.789-00");
    }

    @Test
    void criarUsuarioComEmailDuplicado_retorna409() {
        UserEntity caller = persistUser("Chamador Autenticado 3", "CLIENT");
        String email = uniqueEmail("duplicado");

        String body = """
                {"name":"Usuário Um","email":"%s","phone":"11999990000","role":"CLIENT","password":"senha123"}
                """.formatted(email);

        assertThat(http.post("/users", body, caller.getToken()).status()).isEqualTo(201);

        // segunda tentativa com o mesmo email
        assertThat(http.post("/users", body, caller.getToken()).status()).isEqualTo(409);
    }

    @Test
    void login_comCredenciaisValidas_retornaToken() {
        UserEntity user = persistUser("Carlos", "PROFESSIONAL");

        String loginBody = """
                {"email":"%s","password":"senha123"}
                """.formatted(user.getEmail());

        HttpTestResponse resp = http.post("/users/login", loginBody, null);

        assertThat(resp.status()).isEqualTo(200);
        assertThat(resp.path("token")).isNotNull();
        assertThat(resp.path("email")).isEqualTo(user.getEmail());
    }

    @Test
    void login_comSenhaIncorreta_retorna401() {
        UserEntity user = persistUser("Bia", "CLIENT");

        String loginBody = """
                {"email":"%s","password":"senhaErrada"}
                """.formatted(user.getEmail());

        assertThat(http.post("/users/login", loginBody, null).status()).isEqualTo(401);
    }

    @Test
    void me_comTokenValido_retornaDadosDoUsuarioAutenticado() {
        UserEntity user = persistUser("Token User", "CLIENT");

        HttpTestResponse resp = http.get("/users/me", user.getToken());

        assertThat(resp.status()).isEqualTo(200);
        assertThat(resp.path("email")).isEqualTo(user.getEmail());
    }

    @Test
    void me_semToken_retornaErroDeAutenticacao() {
        // sem Authorization header, o JwtAuthenticationFilter não autentica
        // e o Spring Security bloqueia via .anyRequest().authenticated()
        assertThat(http.get("/users/me", null).status()).isIn(401, 403);
    }

    @Test
    void me_comTokenInvalido_retorna401() {
        assertThat(http.get("/users/me", "token-que-nao-existe").status()).isEqualTo(401);
    }
}