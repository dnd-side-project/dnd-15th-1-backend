-- 둘픽 스키마 V4: 알림·푸시.

CREATE TABLE member_notification_settings
(
    member_id                 BIGINT      NOT NULL,
    content_saved_enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    date_schedule_enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    marketing_enabled         BOOLEAN     NOT NULL DEFAULT FALSE,
    marketing_consent_version VARCHAR(30) NULL,
    created_at                DATETIME(6) NOT NULL,
    updated_at                DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_notification_settings_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE marketing_consent_histories
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    consented       BOOLEAN     NOT NULL,
    consent_version VARCHAR(30) NOT NULL,
    changed_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketing_consent_history_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_marketing_consent_history_member_changed
    ON marketing_consent_histories (member_id, changed_at);

CREATE TABLE push_devices
(
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    member_id                 BIGINT        NOT NULL,
    device_id                 CHAR(36)      NOT NULL,
    platform                  VARCHAR(10)   NOT NULL,
    provider                  VARCHAR(10)   NOT NULL,
    registration_hash         CHAR(64)      NOT NULL,
    encrypted_registration_id VARCHAR(2048) NOT NULL,
    status                    VARCHAR(20)   NOT NULL,
    app_version               VARCHAR(30)   NULL,
    last_registered_at        DATETIME(6)   NOT NULL,
    invalidated_at            DATETIME(6)   NULL,
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
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

CREATE TABLE notifications
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    receiver_member_id BIGINT       NOT NULL,
    type              VARCHAR(40)  NOT NULL,
    title             VARCHAR(100) NOT NULL,
    body              VARCHAR(500) NOT NULL,
    route             VARCHAR(30)  NOT NULL,
    reference_id      VARCHAR(100) NULL,
    deduplication_key VARCHAR(200) NOT NULL,
    read_at           DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notifications_receiver_deduplication
        UNIQUE (receiver_member_id, deduplication_key),
    CONSTRAINT ck_notifications_type
        CHECK (type IN (
            'COUPLE_CONNECTED',
            'COUPLE_DISCONNECTED',
            'CONTENT_SAVE_MILESTONE',
            'DATE_SCHEDULE_REMINDER',
            'MARKETING'
        )),
    CONSTRAINT ck_notifications_route
        CHECK (route IN ('COUPLE_STATUS', 'SAVED_CONTENTS', 'DATE_SCHEDULE', 'NOTICE')),
    CONSTRAINT fk_notifications_receiver
        FOREIGN KEY (receiver_member_id) REFERENCES members (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_receiver_cursor
    ON notifications (receiver_member_id, id DESC);

CREATE INDEX idx_notifications_receiver_unread
    ON notifications (receiver_member_id, read_at);

CREATE TABLE notification_deliveries
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    notification_id     BIGINT       NOT NULL,
    push_device_id      BIGINT       NOT NULL,
    provider            VARCHAR(10)  NOT NULL DEFAULT 'FCM',
    status              VARCHAR(20)  NOT NULL,
    attempt_count       INT          NOT NULL DEFAULT 0,
    next_attempt_at     DATETIME(6)  NOT NULL,
    last_attempted_at   DATETIME(6)  NULL,
    sent_at             DATETIME(6)  NULL,
    provider_message_id VARCHAR(200) NULL,
    last_error_code     VARCHAR(100) NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_deliveries_target
        UNIQUE (notification_id, push_device_id),
    CONSTRAINT ck_notification_deliveries_provider
        CHECK (provider IN ('FCM', 'APNS')),
    CONSTRAINT ck_notification_deliveries_status
        CHECK (status IN ('PENDING', 'SENDING', 'RETRY_PENDING', 'SENT', 'FAILED')),
    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_deliveries_push_device
        FOREIGN KEY (push_device_id) REFERENCES push_devices (id)
);

CREATE INDEX idx_notification_deliveries_retry
    ON notification_deliveries (status, next_attempt_at, id);

CREATE TABLE couple_content_save_counters
(
    couple_id               BIGINT      NOT NULL,
    saver_member_id         BIGINT      NOT NULL,
    save_count              BIGINT      NOT NULL DEFAULT 0,
    last_notified_milestone BIGINT      NOT NULL DEFAULT 0,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (couple_id, saver_member_id),
    CONSTRAINT ck_content_save_counter_count
        CHECK (save_count >= 0 AND last_notified_milestone >= 0),
    CONSTRAINT fk_content_save_counter_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id),
    CONSTRAINT fk_content_save_counter_saver
        FOREIGN KEY (saver_member_id) REFERENCES members (id)
);

CREATE INDEX idx_content_save_counter_saver
    ON couple_content_save_counters (saver_member_id);
