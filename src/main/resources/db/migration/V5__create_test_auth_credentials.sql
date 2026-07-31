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
