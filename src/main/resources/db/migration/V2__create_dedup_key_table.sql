CREATE TABLE dedup_key (
    user_id UUID NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    bucket TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, fingerprint, bucket)
);
