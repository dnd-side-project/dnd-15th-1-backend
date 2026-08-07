CREATE TABLE push_devices
(
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    member_id                   BIGINT        NOT NULL,
    device_id                   CHAR(36)      NOT NULL,
    platform                    VARCHAR(10)   NOT NULL,
    provider                    VARCHAR(10)   NOT NULL,
    registration_hash           CHAR(64)      NOT NULL,
    encrypted_registration_id   VARCHAR(2048) NOT NULL,
    status                      VARCHAR(20)   NOT NULL,
    app_version                 VARCHAR(30)   NULL,
    last_registered_at          DATETIME(6)   NOT NULL,
    invalidated_at              DATETIME(6)   NULL,
    created_at                  DATETIME(6)   NOT NULL,
    updated_at                  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_push_devices_provider_device
        UNIQUE (provider, device_id),
    CONSTRAINT uk_push_devices_provider_registration
        UNIQUE (provider, registration_hash),
    CONSTRAINT ck_push_devices_platform
        CHECK (platform IN ('IOS')),
    CONSTRAINT ck_push_devices_provider
        CHECK (provider IN ('FCM', 'APNS')),
    CONSTRAINT ck_push_devices_status
        CHECK (status IN ('ACTIVE', 'LOGGED_OUT', 'INVALIDATED', 'WITHDRAWN')),
    CONSTRAINT fk_push_devices_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_push_devices_member_status
    ON push_devices (member_id, status);
