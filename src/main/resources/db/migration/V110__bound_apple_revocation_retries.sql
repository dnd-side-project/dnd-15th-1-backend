ALTER TABLE apple_revocation_outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        AFTER attempt_count;

CREATE INDEX idx_apple_revocation_outbox_status_attempt
    ON apple_revocation_outbox (status, attempt_count, next_attempt_at);
