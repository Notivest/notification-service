CREATE TABLE IF NOT EXISTS user_contact (
    user_id       UUID PRIMARY KEY,
    primary_email VARCHAR(320)      NOT NULL,
    email_status  VARCHAR(12)       NOT NULL,
    locale        VARCHAR(10),
    channels_json JSONB             NOT NULL,
    quiet_hours   JSONB,
    version       BIGINT            NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);
