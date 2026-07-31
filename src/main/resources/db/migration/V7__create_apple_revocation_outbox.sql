CREATE TABLE apple_revocation_outbox
(
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    member_id               BIGINT        NOT NULL,
    encrypted_refresh_token VARCHAR(2048) NOT NULL,
    client_id               VARCHAR(255)  NOT NULL,
    attempt_count           INT           NOT NULL DEFAULT 0,
    next_attempt_at         DATETIME(6)   NOT NULL,
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_apple_revocation_outbox_next_attempt
    ON apple_revocation_outbox (next_attempt_at);
