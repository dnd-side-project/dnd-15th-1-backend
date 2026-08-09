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
    CONSTRAINT fk_member_profiles_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE connection_codes
(
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    member_id          BIGINT        NOT NULL,
    code_digest        CHAR(64)      NOT NULL,
    encrypted_code     VARCHAR(255)  NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    issued_reason      VARCHAR(30)   NOT NULL,
    used_at            DATETIME(6)   NULL,
    revoked_at         DATETIME(6)   NULL,
    created_at         DATETIME(6)   NOT NULL,
    active_member_id   BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN member_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_connection_codes_digest UNIQUE (code_digest),
    CONSTRAINT uk_connection_codes_active_member UNIQUE (active_member_id),
    CONSTRAINT fk_connection_codes_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_connection_codes_member_status
    ON connection_codes (member_id, status);
