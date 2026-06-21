package agendo.app.server.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registra explicitamente um ObjectMapper (Jackson 2 — pacote
 * com.fasterxml.jackson) no contexto de teste.
 * Fornecer este bean satisfaz essa injeção sem depender da auto-config do
 * Jackson. Fica em @TestConfiguration porque métodos @Bean só são
 * processados de forma confiável numa classe de configuração de fato.
 */
@TestConfiguration
public class TestJacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}