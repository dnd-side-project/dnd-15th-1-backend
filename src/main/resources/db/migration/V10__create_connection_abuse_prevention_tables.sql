CREATE TABLE connection_rate_limit_subjects
(
    member_id      BIGINT      NOT NULL,
    blocked_until  DATETIME(6) NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_connection_rate_limit_subjects_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE connection_attempts
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL,
    ip_hash     CHAR(64)     NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    outcome     VARCHAR(30)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_connection_attempts_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_connection_attempts_member_action_created
    ON connection_attempts (member_id, action, created_at);

CREATE INDEX idx_connection_attempts_member_outcome_created
    ON connection_attempts (member_id, outcome, created_at);

CREATE INDEX idx_connection_attempts_ip_outcome_created
    ON connection_attempts (ip_hash, outcome, created_at);

CREATE INDEX idx_connection_attempts_created_at
    ON connection_attempts (created_at);
