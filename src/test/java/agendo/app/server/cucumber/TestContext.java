package agendo.app.server.cucumber;

import org.springframework.stereotype.Component;

import agendo.app.server.support.HttpTestClient;
import agendo.app.server.support.HttpTestClient.HttpTestResponse;
import io.cucumber.spring.ScenarioScope;

/**
 * Estado compartilhado entre as classes de step definitions dentro de UM
 * mesmo cenário. @ScenarioScope garante que cada cenário recebe sua própria
 * instância "limpa".
 *
 * Guarda também o HttpTestClient do cenário (configurado no Hooks com a
 * porta aleatória do servidor) e a última resposta HTTP, para que os passos
 * @Então possam fazer asserções sobre status e corpo.
 */
@Component
@ScenarioScope
public class TestContext {

    private HttpTestClient http;
    private HttpTestResponse lastResponse;

    private Long professionalId;
    private String professionalToken;

    private Long clientId;
    private String clientToken;

    private Long serviceTypeId;
    private Long appointmentId;

    private String billingId;
    private String paymentUrl;

    private String lastEmail;

    public HttpTestClient getHttp() {
        return http;
    }

    public void setHttp(HttpTestClient http) {
        this.http = http;
    }

    public HttpTestResponse getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(HttpTestResponse lastResponse) {
        this.lastResponse = lastResponse;
    }

    public Long getProfessionalId() {
        return professionalId;
    }

    public void setProfessionalId(Long professionalId) {
        this.professionalId = professionalId;
    }

    public String getProfessionalToken() {
        return professionalToken;
    }

    public void setProfessionalToken(String professionalToken) {
        this.professionalToken = professionalToken;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public Long getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(Long serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getBillingId() {
        return billingId;
    }

    public void setBillingId(String billingId) {
        this.billingId = billingId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getLastEmail() {
        return lastEmail;
    }

    public void setLastEmail(String lastEmail) {
        this.lastEmail = lastEmail;
    }
}