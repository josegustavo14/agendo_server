package agendo.app.server.modules.payment.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCustomerRequest {

    private String name;
    private String cellphone;
    private String email;
    private String taxId;
}
