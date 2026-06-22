package agendo.app.server.modules.payment.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BillingResponse {
    private BillingData data;
    private String error;

    @Data
    public static class BillingData {
        private String id;
        private String url; /** link de pagamento gerado para o cliente. */
        private String status; /** PENDING | PAID | EXPIRED | CANCELLED */
        private Boolean devMode;
        private List<String> methods;
        private List<ProductData> products;
        private String frequency;
        private Integer amount;  /** total em centavos. */
        private CustomerData customer;
        private Boolean allowCoupons;
        private List<String> coupons;
    }

    @Data
    public static class ProductData {
        private String id;
        private String externalId;
        private Integer quantity;
    }

    @Data
    public static class CustomerData {
        private String id;
        private CustomerMetadata metadata;
    }

    @Data
    public static class CustomerMetadata {
        private String name;
        private String cellphone;
        private String email;
        private String taxId;
    }
}
