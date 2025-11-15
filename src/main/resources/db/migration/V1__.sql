CREATE TABLE dedup_key
(
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    user_id     UUID          NOT NULL,
    fingerprint VARCHAR(128)  NOT NULL,
    bucket      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_dedup_key PRIMARY KEY (user_id, fingerprint, bucket)
);

CREATE TABLE email_event
(
    id                 UUID          NOT NULL,
    user_id            UUID          NULL,
    email              VARCHAR(320)  NOT NULL,
    kind               VARCHAR(24)   NOT NULL,
    provider_reference VARCHAR(128)  NULL,
    payload            JSONB         NULL,
    occurred_at        TIMESTAMPTZ   NOT NULL,
    received_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_email_event PRIMARY KEY (id)
);

CREATE TABLE email_job
(
    id            UUID          NOT NULL,
    user_id       UUID          NOT NULL,
    template_key  VARCHAR(64)   NOT NULL,
    template_json JSONB         NOT NULL,
    status        VARCHAR(16)   NOT NULL,
    attempts      INT           NOT NULL,
    error         VARCHAR(255)  NULL,
    scheduled_at  TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_email_job PRIMARY KEY (id)
);

CREATE TABLE user_contact
(
    user_id       UUID          NOT NULL,
    primary_email VARCHAR(320)  NOT NULL,
    email_status  VARCHAR(12)   NOT NULL,
    locale        VARCHAR(10)   NULL,
    channels_json JSONB         NOT NULL,
    quiet_hours   JSONB         NULL,
    version       BIGINT        NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_user_contact PRIMARY KEY (user_id)
);
