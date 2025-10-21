CREATE TABLE IF NOT EXISTS email_event (
    id UUID PRIMARY KEY,
    user_id UUID,
    email VARCHAR(320) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    provider_reference VARCHAR(128),
    payload JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_email_event_user_kind ON email_event (user_id, kind);
