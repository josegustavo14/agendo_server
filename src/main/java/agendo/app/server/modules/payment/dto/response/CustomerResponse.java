package agendo.app.server.modules.payment.dto.response;

import lombok.Data;

@Data
public class CustomerResponse {
    private CustomerData data;
    private String error;

    @Data
    public static class CustomerData {
        private String id;
        private String name;
        private String cellphone;
        private String email;
        private String taxId;
    }
}