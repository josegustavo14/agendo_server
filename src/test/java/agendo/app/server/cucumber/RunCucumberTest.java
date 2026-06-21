package agendo.app.server.cucumber;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Ponto de entrada que o Gradle/JUnit usa para descobrir e rodar os
 * cenários Cucumber. Basta executar:
 *
 *   ./gradlew test --tests "agendo.app.server.cucumber.RunCucumberTest"
 *
 * ou simplesmente ./gradlew test, que já inclui esta classe junto com os
 * demais testes JUnit 5.
 *
 * @SelectClasspathResource("features") aponta para src/test/resources/features.
 * GLUE_PROPERTY_NAME indica em qual pacote o Cucumber deve procurar as
 * classes de step definitions (@Dado/@Quando/@Então) e hooks.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "agendo.app.server.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunCucumberTest {
}