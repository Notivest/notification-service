CREATE TABLE IF NOT EXISTS email_job (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    template_key VARCHAR(64) NOT NULL,
    template_json JSONB NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    error TEXT,
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_email_job_status_scheduled_at ON email_job (status, scheduled_at);
