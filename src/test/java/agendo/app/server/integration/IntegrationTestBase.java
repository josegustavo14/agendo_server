package agendo.app.server.integration;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import agendo.app.server.modules.payment.client.AbacatePayClient;
import agendo.app.server.modules.user.models.UserEntity;
import agendo.app.server.modules.user.models.UserRole;
import agendo.app.server.modules.user.repository.UserRepository;
import agendo.app.server.support.HttpTestClient;
import agendo.app.server.support.TestJacksonConfig;

/**
 * Classe base para os testes de integração HTTP.
 *
 * As chamadas HTTP são feitas via HttpTestClient (sobre o RestClient do
 * Spring), substituindo o RestAssured — que não é compatível com o Groovy 5
 * trazido pelo Spring Boot 4.
 *
 * OBSERVAÇÃO: POST /users exige autenticação. Para preparar o cenário de um
 * teste, nós não usamos o endpoint HTTP — persistimos o UserEntity diretamente
 * via UserRepository (assim como o DataSeeder faz). O endpoint HTTP de cadastro só
 * é testado de fato dentro do próprio UserIntegrationTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJacksonConfig.class)
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    @MockitoBean
    protected AbacatePayClient abacatePayClient;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected HttpTestClient http;

    @BeforeEach
    void setUpHttpClient() {
        this.http = new HttpTestClient(port);
    }

    /** Gera um e-mail único por chamada, para evitar conflito de "email já cadastrado" entre testes. */
    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    /**
     * Persiste um usuário direto no banco (sem passar pelo endpoint HTTP) e
     * retorna o usuário com token já pronto para uso no header Authorization.
     */
    protected UserEntity persistUser(String name, String role) {
        UserEntity user = UserEntity.builder()
                .name(name)
                .email(uniqueEmail(name.toLowerCase().replace(" ", "-")))
                .phone("11999990000")
                .role(UserRole.valueOf(role))
                .passwordHash(passwordEncoder.encode("senha123"))
                .token(UUID.randomUUID().toString())
                .build();

        return userRepository.save(user);
    }
}