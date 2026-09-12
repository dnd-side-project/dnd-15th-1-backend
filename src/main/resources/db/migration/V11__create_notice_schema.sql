-- 둘픽 스키마 V11: 공지사항과 전체 회원 알림 작업.

CREATE TABLE notices
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE notice_notification_campaigns
(
    id             CHAR(36)     NOT NULL,
    notice_id      BIGINT       NOT NULL,
    title          VARCHAR(100) NOT NULL,
    body           VARCHAR(500) NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    target_count   INT          NOT NULL DEFAULT 0,
    queued_count   INT          NOT NULL DEFAULT 0,
    last_member_id BIGINT       NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    completed_at   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notice_notification_campaign_notice
        UNIQUE (notice_id),
    CONSTRAINT ck_notice_notification_campaign_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_notice_notification_campaign_counts
        CHECK (target_count >= 0 AND queued_count >= 0 AND last_member_id >= 0),
    CONSTRAINT fk_notice_notification_campaign_notice
        FOREIGN KEY (notice_id) REFERENCES notices (id)
);

CREATE INDEX idx_notice_notification_campaign_status_created
    ON notice_notification_campaigns (status, created_at);

ALTER TABLE notifications
    DROP CHECK ck_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN (
            'COUPLE_CONNECTED',
            'COUPLE_DISCONNECTED',
            'CONTENT_SAVE_MILESTONE',
            'DATE_SCHEDULE_REMINDER',
            'MARKETING',
            'ANNOUNCEMENT'
        ));
