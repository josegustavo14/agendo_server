CREATE TABLE appointment_history (
    id              BIGSERIAL PRIMARY KEY,
    appointment_id  BIGINT NOT NULL REFERENCES appointments(id),
    previous_status VARCHAR(20),
    new_status      VARCHAR(20) NOT NULL,
    changed_by_id   BIGINT NOT NULL REFERENCES users(id),
    changed_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
