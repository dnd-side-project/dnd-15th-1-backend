CREATE TABLE members
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    status     VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE social_accounts
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    member_id              BIGINT        NOT NULL,
    provider               VARCHAR(20)   NOT NULL,
    provider_subject       VARCHAR(255)  NOT NULL,
    email                  VARCHAR(320)  NULL,
    provider_refresh_token VARCHAR(2048) NULL,
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
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    member_id              BIGINT       NOT NULL,
    token_hash             CHAR(64)     NOT NULL,
    expires_at             DATETIME(6)  NOT NULL,
    revoked_at             DATETIME(6)  NULL,
    replaced_by_token_hash CHAR(64)     NULL,
    created_at             DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_refresh_tokens_member_id
    ON refresh_tokens (member_id);

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
