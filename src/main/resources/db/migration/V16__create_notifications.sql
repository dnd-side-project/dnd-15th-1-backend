CREATE TABLE notifications
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    receiver_member_id    BIGINT       NOT NULL,
    type                  VARCHAR(40)  NOT NULL,
    title                 VARCHAR(100) NOT NULL,
    body                  VARCHAR(500) NOT NULL,
    route                 VARCHAR(30)  NOT NULL,
    reference_id          VARCHAR(100) NULL,
    deduplication_key     VARCHAR(200) NOT NULL,
    read_at               DATETIME(6)  NULL,
    created_at             DATETIME(6) NOT NULL,
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
        FOREIGN KEY (receiver_member_id) REFERENCES members (id)
);

CREATE INDEX idx_notifications_receiver_cursor
    ON notifications (receiver_member_id, id DESC);

CREATE INDEX idx_notifications_receiver_unread
    ON notifications (receiver_member_id, read_at);

CREATE TABLE notification_deliveries
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    notification_id       BIGINT       NOT NULL,
    push_device_id        BIGINT       NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    attempt_count         INT          NOT NULL DEFAULT 0,
    next_attempt_at       DATETIME(6)  NOT NULL,
    last_attempted_at     DATETIME(6)  NULL,
    sent_at               DATETIME(6)  NULL,
    provider_message_id   VARCHAR(200) NULL,
    last_error_code       VARCHAR(100) NULL,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_deliveries_target
        UNIQUE (notification_id, push_device_id),
    CONSTRAINT ck_notification_deliveries_status
        CHECK (status IN ('PENDING', 'SENDING', 'RETRY_PENDING', 'SENT', 'FAILED')),
    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_deliveries_push_device
        FOREIGN KEY (push_device_id) REFERENCES push_devices (id)
);

CREATE INDEX idx_notification_deliveries_retry
    ON notification_deliveries (status, next_attempt_at, id);
