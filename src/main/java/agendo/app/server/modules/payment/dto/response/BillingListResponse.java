package agendo.app.server.modules.payment.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BillingListResponse {
    private List<BillingResponse.BillingData> data;
    private String error;
}
