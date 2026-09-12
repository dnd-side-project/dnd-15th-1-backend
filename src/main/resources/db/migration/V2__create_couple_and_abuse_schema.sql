-- 둘픽 스키마 V2: 커플·연결·접속 제한.

CREATE TABLE connection_codes
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    code_digest      CHAR(64)     NOT NULL,
    encrypted_code   VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    issued_reason    VARCHAR(30)  NOT NULL,
    used_at          DATETIME(6)  NULL,
    revoked_at       DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    active_member_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN member_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_connection_codes_digest
        UNIQUE (code_digest),
    CONSTRAINT uk_connection_codes_active_member
        UNIQUE (active_member_id),
    CONSTRAINT fk_connection_codes_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_connection_codes_member_status
    ON connection_codes (member_id, status);

CREATE TABLE couples
(
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    status                    VARCHAR(20) NOT NULL,
    connected_at              DATETIME(6) NOT NULL,
    disconnected_at           DATETIME(6) NULL,
    disconnected_by_member_id BIGINT      NULL,
    created_at                DATETIME(6) NOT NULL,
    updated_at                DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_couples_disconnected_by_member
        FOREIGN KEY (disconnected_by_member_id) REFERENCES members (id)
);

CREATE TABLE active_couple_members
(
    member_id BIGINT      NOT NULL,
    couple_id BIGINT      NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT uk_active_couple_members_couple_member
        UNIQUE (couple_id, member_id),
    CONSTRAINT fk_active_couple_members_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_active_couple_members_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id)
);

CREATE INDEX idx_active_couple_members_couple_id
    ON active_couple_members (couple_id);

CREATE TABLE connection_rate_limit_subjects
(
    member_id     BIGINT      NOT NULL,
    blocked_until DATETIME(6) NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_connection_rate_limit_subjects_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE connection_attempts
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    ip_hash    CHAR(64)    NULL,
    action     VARCHAR(30) NOT NULL,
    outcome    VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
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

