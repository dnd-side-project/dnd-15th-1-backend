-- 둘픽 스키마 V9: 마케팅 알림 캠페인.

CREATE TABLE marketing_notification_campaigns
(
    id            CHAR(36)     NOT NULL,
    title         VARCHAR(100) NOT NULL,
    body          VARCHAR(500) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    target_count  INT          NOT NULL DEFAULT 0,
    queued_count  INT          NOT NULL DEFAULT 0,
    last_member_id BIGINT      NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    completed_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_marketing_campaign_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_marketing_campaign_counts
        CHECK (target_count >= 0 AND queued_count >= 0 AND last_member_id >= 0)
);

CREATE INDEX idx_marketing_campaign_status_created
    ON marketing_notification_campaigns (status, created_at);
