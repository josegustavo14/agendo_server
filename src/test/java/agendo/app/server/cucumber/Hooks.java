package agendo.app.server.cucumber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import agendo.app.server.support.HttpTestClient;
import io.cucumber.java.Before;

/**
 * Hook executado antes de cada cenário Cucumber. Inicializa o HttpTestClient
 * com a porta aleatória do servidor e o disponibiliza no TestContext, para
 * que os steps façam chamadas HTTP via RestClient (sem RestAssured).
 */
public class Hooks {

    @LocalServerPort
    private int port;

    @Autowired
    private TestContext context;

    @Before
    public void configurarHttpClient() {
        context.setHttp(new HttpTestClient(port));
    }
}