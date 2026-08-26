-- 둘픽 스키마 V5: 피드백·프로필 제약.

CREATE TABLE member_feedbacks
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    member_id         BIGINT        NOT NULL,
    client_request_id CHAR(36)      NOT NULL,
    type              VARCHAR(30)   NOT NULL,
    content           VARCHAR(1000) NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_feedbacks_member_request
        UNIQUE (member_id, client_request_id),
    CONSTRAINT ck_member_feedbacks_type
        CHECK (type IN ('INQUIRY', 'BUG_REPORT', 'FEATURE_SUGGESTION', 'OTHER')),
    CONSTRAINT ck_member_feedbacks_status
        CHECK (status IN ('RECEIVED', 'IN_REVIEW', 'RESOLVED')),
    CONSTRAINT fk_member_feedbacks_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_member_feedbacks_member_created
    ON member_feedbacks (member_id, created_at);

ALTER TABLE member_profiles
    MODIFY COLUMN indoor_outdoor VARCHAR(20) NULL,
    MODIFY COLUMN activity_level VARCHAR(20) NULL,
    MODIFY COLUMN date_time VARCHAR(20) NULL,
    MODIFY COLUMN date_focus VARCHAR(20) NULL;

ALTER TABLE member_profiles
    ADD CONSTRAINT ck_member_profiles_date_preferences_complete
        CHECK (
            (
                indoor_outdoor IS NULL
                AND activity_level IS NULL
                AND date_time IS NULL
                AND date_focus IS NULL
            )
            OR
            (
                indoor_outdoor IS NOT NULL
                AND activity_level IS NOT NULL
                AND date_time IS NOT NULL
                AND date_focus IS NOT NULL
            )
        );

