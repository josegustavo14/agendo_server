package agendo.app.server.modules.payment.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class CustomerListResponse {
    private List<CustomerResponse.CustomerData> data;
    private String error;
}
