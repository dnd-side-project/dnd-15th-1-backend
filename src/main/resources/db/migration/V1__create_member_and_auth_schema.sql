CREATE TABLE members
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    status             VARCHAR(20) NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    last_withdrawn_at  DATETIME(6) NULL,
    last_rejoined_at   DATETIME(6) NULL,
    token_version      BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE member_profiles
(
    member_id       BIGINT      NOT NULL,
    nickname        VARCHAR(64) NOT NULL,
    profile_icon    TINYINT     NOT NULL,
    indoor_outdoor  VARCHAR(20) NOT NULL,
    activity_level  VARCHAR(20) NOT NULL,
    date_time       VARCHAR(20) NOT NULL,
    date_focus      VARCHAR(20) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT ck_member_profiles_profile_icon
        CHECK (profile_icon BETWEEN 1 AND 5),
    CONSTRAINT ck_member_profiles_indoor_outdoor
        CHECK (indoor_outdoor IN ('INDOOR', 'OUTDOOR')),
    CONSTRAINT ck_member_profiles_activity_level
        CHECK (activity_level IN ('ACTIVE', 'STATIC')),
    CONSTRAINT ck_member_profiles_date_time
        CHECK (date_time IN ('DAY', 'NIGHT')),
    CONSTRAINT ck_member_profiles_date_focus
        CHECK (date_focus IN ('FOOD', 'SIGHTSEEING')),
    CONSTRAINT fk_member_profiles_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE social_accounts
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    member_id              BIGINT        NOT NULL,
    provider               VARCHAR(20)   NOT NULL,
    provider_subject       VARCHAR(255)  NOT NULL,
    email                  VARCHAR(320)  NULL,
    provider_refresh_token VARCHAR(2048) NULL,
    provider_client_id     VARCHAR(255)  NULL,
    created_at             DATETIME(6)   NOT NULL,
    updated_at             DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_accounts_provider_subject
        UNIQUE (provider, provider_subject),
    CONSTRAINT fk_social_accounts_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_social_accounts_member_id
    ON social_accounts (member_id);

CREATE TABLE refresh_tokens
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    member_id              BIGINT      NOT NULL,
    token_hash             CHAR(64)    NOT NULL,
    expires_at             DATETIME(6) NOT NULL,
    revoked_at             DATETIME(6) NULL,
    replaced_by_token_hash CHAR(64)    NULL,
    created_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_refresh_tokens_member_id
    ON refresh_tokens (member_id);

CREATE INDEX idx_refresh_tokens_member_revoked
    ON refresh_tokens (member_id, revoked_at);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

CREATE INDEX idx_refresh_tokens_revoked_at
    ON refresh_tokens (revoked_at);

CREATE TABLE login_nonces
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    provider   VARCHAR(20) NOT NULL,
    nonce_hash CHAR(64)    NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_login_nonces_provider_hash
        UNIQUE (provider, nonce_hash)
);

CREATE INDEX idx_login_nonces_expires_at
    ON login_nonces (expires_at);

CREATE INDEX idx_login_nonces_used_at
    ON login_nonces (used_at);

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

CREATE TABLE test_auth_credentials
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_test_auth_credentials_member
        UNIQUE (member_id),
    CONSTRAINT uk_test_auth_credentials_email
        UNIQUE (email),
    CONSTRAINT fk_test_auth_credentials_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);
