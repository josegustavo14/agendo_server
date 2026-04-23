package agendo.app.server.modules.payment.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AbacatePayFeignConfig {

    @Value("${abacatepay.api-key}")
    private String apiKey;

    @Bean
    public RequestInterceptor abacatePayRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Authorization", "Bearer " + apiKey);
            requestTemplate.header("Content-Type", "application/json");
        };
    }
}