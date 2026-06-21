package agendo.app.server.cucumber;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import agendo.app.server.modules.payment.client.AbacatePayClient;
import agendo.app.server.support.TestJacksonConfig;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Ponte entre o Cucumber e o contexto de aplicação do Spring Boot.
 *
 * <p>Esta é a única classe da suíte de aceitação anotada com
 * {@link CucumberContextConfiguration}. Por contrato do cucumber-spring,
 * deve existir exatamente uma classe com essa anotação no glue path: é ela
 * que define qual contexto Spring será carregado e compartilhado por todos
 * os cenários. As classes de step definitions (CadastroLoginSteps,
 * AgendamentoSteps, PagamentoSteps) e o {@code Hooks} são automaticamente
 * gerenciados como beans Spring graças à dependência cucumber-spring no
 * classpath — não precisam ser registrados aqui manualmente.</p>
 *
 * <p>O contexto é iniciado uma única vez e reaproveitado entre cenários para
 * acelerar a execução; o isolamento de estado por cenário é garantido pelo
 * {@code TestContext}, que é {@code @ScenarioScope}.</p>
 *
 * <h2>Anotações</h2>
 * <ul>
 *   <li>{@link CucumberContextConfiguration} — marca esta classe como a
 *       configuração de contexto que o Cucumber deve usar.</li>
 *   <li>{@link SpringBootTest} com
 *       {@link SpringBootTest.WebEnvironment#RANDOM_PORT} — sobe a aplicação
 *       completa num servidor embarcado real, em uma porta aleatória, para
 *       que os cenários façam chamadas HTTP de verdade (via HttpTestClient,
 *       sobre o RestClient do Spring).</li>
 *   <li>{@link ActiveProfiles}{@code ("test")} — ativa o perfil de teste,
 *       que usa H2 em memória (modo PostgreSQL), desliga o Flyway e o
 *       DataSeeder, e fornece credenciais fake da AbacatePay.</li>
 *   <li>{@link Import}{@code (TestJacksonConfig.class)} — registra um bean
 *       {@code com.fasterxml.jackson.databind.ObjectMapper} (Jackson 2),
 *       necessário porque o Spring Boot 4 auto-configura apenas um
 *       {@code JsonMapper} de Jackson 3 ({@code tools.jackson}), enquanto as
 *       classes de step injetam o ObjectMapper de Jackson 2.</li>
 * </ul>
 *
 * <h2>Mock da AbacatePay</h2>
 * <p>O {@link AbacatePayClient} (um {@code @FeignClient}) é substituído por
 * um mock via {@link MockitoBean}, de modo que os cenários de pagamento não
 * dependam da API externa da AbacatePay estar disponível. Cada cenário
 * configura o comportamento esperado do mock antes de exercitar o endpoint
 * (por exemplo, em PagamentoSteps, via {@code when(...).thenReturn(...)}).</p>
 *
 * @see TestJacksonConfig
 * @see io.cucumber.spring.ScenarioScope
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJacksonConfig.class)
public class CucumberSpringConfiguration {

    @MockitoBean
    private AbacatePayClient abacatePayClient;
}