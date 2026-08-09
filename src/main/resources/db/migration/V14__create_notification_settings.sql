CREATE TABLE member_notification_settings
(
    member_id               BIGINT      NOT NULL,
    content_saved_enabled   BOOLEAN     NOT NULL DEFAULT TRUE,
    date_schedule_enabled   BOOLEAN     NOT NULL DEFAULT TRUE,
    marketing_enabled       BOOLEAN     NOT NULL DEFAULT FALSE,
    marketing_consent_version VARCHAR(30) NULL,
    created_at              DATETIME(6) NOT NULL,
    updated_at              DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_notification_settings_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE marketing_consent_histories
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    member_id         BIGINT      NOT NULL,
    consented         BOOLEAN     NOT NULL,
    consent_version   VARCHAR(30) NOT NULL,
    changed_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketing_consent_history_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_marketing_consent_history_member_changed
    ON marketing_consent_histories (member_id, changed_at);
