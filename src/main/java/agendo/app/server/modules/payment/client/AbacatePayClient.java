package agendo.app.server.modules.payment.client;

import agendo.app.server.modules.payment.config.AbacatePayFeignConfig;
import agendo.app.server.modules.payment.dto.request.CreateBillingRequest;
import agendo.app.server.modules.payment.dto.request.CreateCustomerRequest;
import agendo.app.server.modules.payment.dto.response.BillingListResponse;
import agendo.app.server.modules.payment.dto.response.BillingResponse;
import agendo.app.server.modules.payment.dto.response.CustomerListResponse;
import agendo.app.server.modules.payment.dto.response.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * FeignClient para a API v1 da AbacatePay.
 * base URL: https://api.abacatepay.com/v1
 */
@FeignClient(
        name = "abacatepay",
        url = "${abacatepay.base-url}",
        configuration = AbacatePayFeignConfig.class
)
public interface AbacatePayClient {

    @PostMapping("/customer/create")
    CustomerResponse createCustomer(@RequestBody CreateCustomerRequest request);

    @GetMapping("/customer/list")
    CustomerListResponse listCustomers();

    @PostMapping("/billing/create")
    BillingResponse createBilling(@RequestBody CreateBillingRequest request);

    @GetMapping("/billing/list")
    BillingListResponse listBillings();
}
