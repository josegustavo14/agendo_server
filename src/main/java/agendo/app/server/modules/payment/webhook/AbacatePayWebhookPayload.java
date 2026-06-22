package agendo.app.server.modules.payment.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Payload recebido nos webhooks da AbacatePay v1.
 * A AbacatePay pode adicionar novos campos sem aviso prévio,
 * por isso usamos @JsonIgnoreProperties(ignoreUnknown = true).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbacatePayWebhookPayload {


    private String id;
    private String event;
    private Boolean devMode;
    private DataWrapper data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        private BillingData billing;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillingData {
        private String id;
        private String externalId;
        private Integer amount;
        private Integer paidAmount;
        private String status;
        private CustomerRef customer;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerRef {
        private String id;
    }
}


