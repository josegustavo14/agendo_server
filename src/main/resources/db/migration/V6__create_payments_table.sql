CREATE TABLE payments (
    id                    BIGSERIAL PRIMARY KEY,
    abacatepay_billing_id VARCHAR(100) NOT NULL UNIQUE,
    payment_url           TEXT         NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    amount_in_cents       INTEGER      NOT NULL,
    appointment_id        BIGINT       NOT NULL REFERENCES appointments(id),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP
);

CREATE INDEX idx_payments_billing_id ON payments(abacatepay_billing_id);
CREATE UNIQUE INDEX idx_payments_appointment ON payments(appointment_id);