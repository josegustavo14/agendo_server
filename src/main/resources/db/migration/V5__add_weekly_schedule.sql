CREATE TABLE weekly_schedule (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES users(id),
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 60,
    UNIQUE (professional_id, day_of_week)
);
