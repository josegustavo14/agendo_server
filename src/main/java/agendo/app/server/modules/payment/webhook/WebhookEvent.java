package agendo.app.server.modules.payment.webhook;

public enum WebhookEvent {

    BILLING_CREATED("billing.created"),
    BILLING_PAID("billing.paid"),
    BILLING_FAILED("billing.failed"),
    BILLING_REFUNDED("billing.refunded"),
    SUBSCRIPTION_CREATED("subscription.created"),
    SUBSCRIPTION_CANCELED("subscription.canceled"),
    UNKNOWN("unknown");

    private final String value;

    WebhookEvent(String value) {
        this.value = value;
    }

    public static WebhookEvent fromString(String raw) {
        if (raw == null) return UNKNOWN;
        for (WebhookEvent e : values()) {
            if (e.value.equalsIgnoreCase(raw)) return e;
        }
        return UNKNOWN;
    }

    public String getValue() {
        return value;
    }
}

