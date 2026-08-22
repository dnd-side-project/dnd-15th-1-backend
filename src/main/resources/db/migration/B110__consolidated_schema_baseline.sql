-- Consolidated final schema baseline for version 110.
-- This baseline is used only for new databases; existing databases keep their versioned history.

-- Source: V1__create_member_and_auth_schema.sql
CREATE TABLE members
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    status             VARCHAR(20) NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    last_withdrawn_at  DATETIME(6) NULL,
    last_rejoined_at   DATETIME(6) NULL,
    token_version      BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

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
    CONSTRAINT ck_member_profiles_indoor_outdoor
        CHECK (indoor_outdoor IN ('INDOOR', 'OUTDOOR')),
    CONSTRAINT ck_member_profiles_activity_level
        CHECK (activity_level IN ('ACTIVE', 'STATIC')),
    CONSTRAINT ck_member_profiles_date_time
        CHECK (date_time IN ('DAY', 'NIGHT')),
    CONSTRAINT ck_member_profiles_date_focus
        CHECK (date_focus IN ('FOOD', 'SIGHTSEEING')),
    CONSTRAINT fk_member_profiles_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE social_accounts
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    member_id              BIGINT        NOT NULL,
    provider               VARCHAR(20)   NOT NULL,
    provider_subject       VARCHAR(255)  NOT NULL,
    email                  VARCHAR(320)  NULL,
    provider_refresh_token VARCHAR(2048) NULL,
    provider_client_id     VARCHAR(255)  NULL,
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
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    member_id              BIGINT      NOT NULL,
    token_hash             CHAR(64)    NOT NULL,
    expires_at             DATETIME(6) NOT NULL,
    revoked_at             DATETIME(6) NULL,
    replaced_by_token_hash CHAR(64)    NULL,
    created_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash
        UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_refresh_tokens_member_id
    ON refresh_tokens (member_id);

CREATE INDEX idx_refresh_tokens_member_revoked
    ON refresh_tokens (member_id, revoked_at);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

CREATE INDEX idx_refresh_tokens_revoked_at
    ON refresh_tokens (revoked_at);

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

CREATE INDEX idx_login_nonces_expires_at
    ON login_nonces (expires_at);

CREATE INDEX idx_login_nonces_used_at
    ON login_nonces (used_at);

CREATE TABLE apple_revocation_outbox
(
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    member_id               BIGINT        NOT NULL,
    encrypted_refresh_token VARCHAR(2048) NOT NULL,
    client_id               VARCHAR(255)  NOT NULL,
    attempt_count           INT           NOT NULL DEFAULT 0,
    next_attempt_at         DATETIME(6)   NOT NULL,
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_apple_revocation_outbox_next_attempt
    ON apple_revocation_outbox (next_attempt_at);

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

-- Source: V2__create_couple_schema.sql
CREATE TABLE connection_codes
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    code_digest      CHAR(64)     NOT NULL,
    encrypted_code   VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    issued_reason    VARCHAR(30)  NOT NULL,
    used_at          DATETIME(6)  NULL,
    revoked_at       DATETIME(6)  NULL,
    created_at       DATETIME(6)  NOT NULL,
    active_member_id BIGINT GENERATED ALWAYS AS
        (CASE WHEN status = 'ACTIVE' THEN member_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_connection_codes_digest
        UNIQUE (code_digest),
    CONSTRAINT uk_connection_codes_active_member
        UNIQUE (active_member_id),
    CONSTRAINT fk_connection_codes_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_connection_codes_member_status
    ON connection_codes (member_id, status);

CREATE TABLE couples
(
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    status                    VARCHAR(20) NOT NULL,
    connected_at              DATETIME(6) NOT NULL,
    disconnected_at           DATETIME(6) NULL,
    disconnected_by_member_id BIGINT      NULL,
    created_at                DATETIME(6) NOT NULL,
    updated_at                DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_couples_disconnected_by_member
        FOREIGN KEY (disconnected_by_member_id) REFERENCES members (id)
);

CREATE TABLE active_couple_members
(
    member_id BIGINT      NOT NULL,
    couple_id BIGINT      NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT uk_active_couple_members_couple_member
        UNIQUE (couple_id, member_id),
    CONSTRAINT fk_active_couple_members_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_active_couple_members_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id)
);

CREATE INDEX idx_active_couple_members_couple_id
    ON active_couple_members (couple_id);

CREATE TABLE connection_rate_limit_subjects
(
    member_id     BIGINT      NOT NULL,
    blocked_until DATETIME(6) NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT fk_connection_rate_limit_subjects_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE connection_attempts
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    ip_hash    CHAR(64)    NULL,
    action     VARCHAR(30) NOT NULL,
    outcome    VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_connection_attempts_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_connection_attempts_member_action_created
    ON connection_attempts (member_id, action, created_at);

CREATE INDEX idx_connection_attempts_member_outcome_created
    ON connection_attempts (member_id, outcome, created_at);

CREATE INDEX idx_connection_attempts_ip_outcome_created
    ON connection_attempts (ip_hash, outcome, created_at);

CREATE INDEX idx_connection_attempts_created_at
    ON connection_attempts (created_at);

-- Source: V3__create_place_and_content_schema.sql
CREATE TABLE places
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    kakao_place_id      VARCHAR(80)    NOT NULL,
    name                VARCHAR(255)   NOT NULL,
    address             VARCHAR(500)   NOT NULL,
    road_address        VARCHAR(500)   NULL,
    latitude            DECIMAL(10, 7) NULL,
    longitude           DECIMAL(10, 7) NULL,
    category            VARCHAR(100)   NULL,
    category_group_code VARCHAR(3)     NULL,
    thumbnail_url       VARCHAR(1000)  NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_places_kakao_place_id
        UNIQUE (kakao_place_id)
);

CREATE TABLE place_images
(
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    place_id        BIGINT        NOT NULL,
    image_url       VARCHAR(2000) NOT NULL,
    image_url_hash  CHAR(64)      NOT NULL,
    display_order   INT           NOT NULL,
    source_provider VARCHAR(30)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_place_images_place_url
        UNIQUE (place_id, image_url_hash),
    CONSTRAINT fk_place_images_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE
);

CREATE INDEX idx_place_images_place_order
    ON place_images (place_id, display_order);

CREATE TABLE contents
(
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    canonical_url             VARCHAR(1000) NOT NULL,
    canonical_url_hash        CHAR(64)      NOT NULL,
    source_type               VARCHAR(30)   NOT NULL,
    title                     VARCHAR(4000) NULL,
    content                   TEXT          NULL,
    thumbnail_url             VARCHAR(1000) NULL,
    publication_status        VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    created_at                DATETIME(6)   NOT NULL,
    updated_at                DATETIME(6)   NOT NULL,
    last_checked_at           DATETIME(6)   NULL,
    content_hash              CHAR(64)      NULL,
    place_count               INT           NOT NULL DEFAULT 0,
    source_author_name        VARCHAR(255)  NULL,
    source_author_username    VARCHAR(255)  NULL,
    source_published_on       DATE          NULL,
    like_count                BIGINT        NULL,
    comment_count             BIGINT        NULL,
    engagement_checked_at     DATETIME(6)   NULL,
    analyzer_model            VARCHAR(100)  NULL,
    prompt_version            VARCHAR(50)   NULL,
    extracted_candidates_json TEXT          NULL,
    analyzed_at               DATETIME(6)   NULL,
    analysis_content_hash     VARCHAR(64)   NULL,
    analysis_status           VARCHAR(30)   NULL,
    analysis_started_at       TIMESTAMP(6)  NULL,
    analysis_claim_token      VARCHAR(36)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_contents_url_hash
        UNIQUE (canonical_url_hash)
);

CREATE INDEX idx_contents_publication_created
    ON contents (publication_status, created_at DESC);

CREATE INDEX idx_contents_analysis_claim
    ON contents (canonical_url_hash, content_hash, analysis_status, analysis_started_at);

CREATE TABLE place_imports
(
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    member_id                BIGINT        NOT NULL,
    content_id               BIGINT        NULL,
    canonical_url            VARCHAR(1000) NOT NULL,
    canonical_url_hash       CHAR(64)      NOT NULL,
    source_type              VARCHAR(30)   NOT NULL,
    content_hash             CHAR(64)      NULL,
    source_updated_at        DATETIME(6)   NULL,
    source_author_name       VARCHAR(255)  NULL,
    source_author_username   VARCHAR(255)  NULL,
    source_published_on      DATE          NULL,
    like_count               BIGINT        NULL,
    comment_count            BIGINT        NULL,
    engagement_checked_at    DATETIME(6)   NULL,
    title                    VARCHAR(4000) NULL,
    content                  TEXT          NULL,
    thumbnail_url            VARCHAR(1000) NULL,
    status                   VARCHAR(30)   NOT NULL,
    processing_claim_token   VARCHAR(36)   NULL,
    failure_code             VARCHAR(80)   NULL,
    retry_count              INT           NOT NULL DEFAULT 0,
    created_at               DATETIME(6)   NOT NULL,
    updated_at               DATETIME(6)   NOT NULL,
    completed_at             DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_place_imports_member_url
        UNIQUE (member_id, canonical_url_hash),
    CONSTRAINT fk_place_imports_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_place_imports_content
        FOREIGN KEY (content_id) REFERENCES contents (id)
);

CREATE INDEX idx_place_imports_member_created_at
    ON place_imports (member_id, created_at DESC);

CREATE INDEX idx_place_imports_status_updated_at
    ON place_imports (status, updated_at, id);

CREATE TABLE place_candidates
(
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    import_id              BIGINT        NOT NULL,
    place_id               BIGINT        NULL,
    extracted_name         VARCHAR(255)  NOT NULL,
    extracted_address_hint VARCHAR(500)  NULL,
    evidence               VARCHAR(1000) NULL,
    mention_type           VARCHAR(40)   NULL,
    verification_status    VARCHAR(30)   NOT NULL,
    created_at             DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_place_candidates_import
        FOREIGN KEY (import_id) REFERENCES place_imports (id),
    CONSTRAINT fk_place_candidates_place
        FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE TABLE member_places
(
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    member_id        BIGINT        NOT NULL,
    place_id         BIGINT        NOT NULL,
    source_import_id BIGINT        NULL,
    alias            VARCHAR(100)  NULL,
    memo             VARCHAR(1000) NULL,
    saved_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_places_member_place
        UNIQUE (member_id, place_id),
    CONSTRAINT fk_member_places_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_places_place
        FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT fk_member_places_import
        FOREIGN KEY (source_import_id) REFERENCES place_imports (id)
);

CREATE INDEX idx_member_places_member_saved_at
    ON member_places (member_id, saved_at DESC);

CREATE TABLE content_places
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    content_id BIGINT      NOT NULL,
    place_id   BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_places_content_place
        UNIQUE (content_id, place_id),
    CONSTRAINT fk_content_places_content
        FOREIGN KEY (content_id) REFERENCES contents (id),
    CONSTRAINT fk_content_places_place
        FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE INDEX idx_content_places_content
    ON content_places (content_id, created_at DESC);

CREATE INDEX idx_content_places_place_content
    ON content_places (place_id, content_id);

CREATE TABLE content_submissions
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    content_id   BIGINT      NOT NULL,
    member_id    BIGINT      NULL,
    submitted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_submissions_member_content
        UNIQUE (content_id, member_id),
    CONSTRAINT fk_content_submissions_content
        FOREIGN KEY (content_id) REFERENCES contents (id),
    CONSTRAINT fk_content_submissions_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE SET NULL
);

-- Source: V4__create_notification_schema.sql
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

-- Source: V5__create_feedback_schema.sql
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

-- Source: V6__allow_unset_member_date_preferences.sql
ALTER TABLE member_profiles
    MODIFY COLUMN indoor_outdoor VARCHAR(20) NULL,
    MODIFY COLUMN activity_level VARCHAR(20) NULL,
    MODIFY COLUMN date_time VARCHAR(20) NULL,
    MODIFY COLUMN date_focus VARCHAR(20) NULL;

-- Source: V7__enforce_complete_member_date_preferences.sql
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

-- Source: V100__create_date_course_schema.sql
CREATE TABLE date_courses
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    couple_id            BIGINT       NOT NULL,
    created_by_member_id BIGINT       NOT NULL,
    title                VARCHAR(120) NOT NULL,
    scheduled_at         DATETIME(6)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_date_courses_status
        CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT fk_date_courses_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id),
    CONSTRAINT fk_date_courses_creator
        FOREIGN KEY (created_by_member_id) REFERENCES members (id)
);

CREATE INDEX idx_date_courses_couple_status_scheduled
    ON date_courses (couple_id, status, scheduled_at);

CREATE INDEX idx_date_courses_couple_scheduled
    ON date_courses (couple_id, scheduled_at);

CREATE TABLE date_course_places
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    date_course_id BIGINT      NOT NULL,
    place_id       BIGINT      NOT NULL,
    sequence_order INT         NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_date_course_places_course_order
        UNIQUE (date_course_id, sequence_order),
    CONSTRAINT uk_date_course_places_course_place
        UNIQUE (date_course_id, place_id),
    CONSTRAINT fk_date_course_places_course
        FOREIGN KEY (date_course_id) REFERENCES date_courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_date_course_places_place
        FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE INDEX idx_date_course_places_course_order
    ON date_course_places (date_course_id, sequence_order);

-- Source: V101__create_recent_search_schema.sql
CREATE TABLE recent_searches
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    search_type      VARCHAR(20)  NOT NULL,
    keyword          VARCHAR(200) NOT NULL,
    normalized_query VARCHAR(200) NOT NULL,
    searched_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recent_searches_member_type_query
        UNIQUE (member_id, search_type, normalized_query),
    CONSTRAINT fk_recent_searches_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT ck_recent_searches_type
        CHECK (search_type IN ('CONTENT', 'PLACE'))
);

CREATE INDEX idx_recent_searches_member_type_searched
    ON recent_searches (member_id, search_type, searched_at DESC, id DESC);

-- Source: V102__extend_place_kakao_details.sql
ALTER TABLE places
    ADD COLUMN phone           VARCHAR(50)   NULL AFTER category_group_code,
    ADD COLUMN kakao_place_url VARCHAR(1000) NULL AFTER phone;

-- Source: V104__create_place_classification_schema.sql
CREATE TABLE place_classifications
(
    place_id                  BIGINT      NOT NULL,
    environment_type         VARCHAR(20) NULL,
    environment_source       VARCHAR(20) NULL,
    activity_type            VARCHAR(20) NULL,
    activity_source          VARCHAR(20) NULL,
    time_type                VARCHAR(20) NULL,
    time_source              VARCHAR(20) NULL,
    focus_type               VARCHAR(20) NULL,
    focus_source             VARCHAR(20) NULL,
    created_at               DATETIME(6) NOT NULL,
    updated_at               DATETIME(6) NOT NULL,
    PRIMARY KEY (place_id),
    CONSTRAINT fk_place_classifications_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE,
    CONSTRAINT ck_place_classifications_environment_value
        CHECK (environment_type IS NULL OR environment_type IN ('INDOOR', 'OUTDOOR')),
    CONSTRAINT ck_place_classifications_activity_value
        CHECK (activity_type IS NULL OR activity_type IN ('ACTIVE', 'STATIC')),
    CONSTRAINT ck_place_classifications_time_value
        CHECK (time_type IS NULL OR time_type IN ('DAY', 'NIGHT')),
    CONSTRAINT ck_place_classifications_focus_value
        CHECK (focus_type IS NULL OR focus_type IN ('FOOD', 'SIGHTSEEING')),
    CONSTRAINT ck_place_classifications_environment_pair
        CHECK ((environment_type IS NULL AND environment_source IS NULL)
            OR (environment_type IS NOT NULL AND environment_source IN ('AI', 'MANUAL'))),
    CONSTRAINT ck_place_classifications_activity_pair
        CHECK ((activity_type IS NULL AND activity_source IS NULL)
            OR (activity_type IS NOT NULL AND activity_source IN ('AI', 'MANUAL'))),
    CONSTRAINT ck_place_classifications_time_pair
        CHECK ((time_type IS NULL AND time_source IS NULL)
            OR (time_type IS NOT NULL AND time_source IN ('AI', 'MANUAL'))),
    CONSTRAINT ck_place_classifications_focus_pair
        CHECK ((focus_type IS NULL AND focus_source IS NULL)
            OR (focus_type IS NOT NULL AND focus_source IN ('AI', 'MANUAL')))
);

-- Source: V105__add_content_and_place_fulltext_indexes.sql
ALTER TABLE contents
    ADD FULLTEXT INDEX ft_contents_title_content (title, content) WITH PARSER ngram;

ALTER TABLE places
    ADD FULLTEXT INDEX ft_places_name_address (name, address, road_address) WITH PARSER ngram;

-- Source: V107__create_walking_route_cache.sql
CREATE TABLE walking_route_cache
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    from_place_id    BIGINT         NOT NULL,
    to_place_id      BIGINT         NOT NULL,
    from_latitude    DECIMAL(10, 7) NOT NULL,
    from_longitude   DECIMAL(10, 7) NOT NULL,
    to_latitude      DECIMAL(10, 7) NOT NULL,
    to_longitude     DECIMAL(10, 7) NOT NULL,
    distance_meters  INT            NOT NULL,
    duration_seconds INT            NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_walking_route_cache_place_pair
        UNIQUE (from_place_id, to_place_id),
    CONSTRAINT fk_walking_route_cache_from_place
        FOREIGN KEY (from_place_id) REFERENCES places (id) ON DELETE CASCADE,
    CONSTRAINT fk_walking_route_cache_to_place
        FOREIGN KEY (to_place_id) REFERENCES places (id) ON DELETE CASCADE
);

CREATE INDEX idx_walking_route_cache_from_place
    ON walking_route_cache (from_place_id);

-- Source: V108__create_place_image_enrichment_backlog.sql
CREATE TABLE place_image_enrichment_backlogs
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    place_id         BIGINT       NOT NULL,
    kakao_place_id   VARCHAR(80)  NOT NULL,
    reason           VARCHAR(30)  NOT NULL,
    attempt_count    INT          NOT NULL DEFAULT 0,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    first_failed_at  DATETIME(6)  NOT NULL,
    last_failed_at   DATETIME(6)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_place_image_enrichment_backlogs_place
        UNIQUE (place_id),
    CONSTRAINT fk_place_image_enrichment_backlogs_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE
);

CREATE INDEX idx_place_image_enrichment_backlogs_status_failed
    ON place_image_enrichment_backlogs (status, last_failed_at, id);

-- Source: V109__normalize_optional_place_fields.sql
UPDATE places
SET road_address = NULL
WHERE road_address IS NOT NULL
  AND TRIM(road_address) = '';

UPDATE member_places
SET alias = NULL
WHERE alias IS NOT NULL
  AND TRIM(alias) = '';

-- Source: V110__bound_apple_revocation_retries.sql
ALTER TABLE apple_revocation_outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        AFTER attempt_count;

CREATE INDEX idx_apple_revocation_outbox_status_attempt
    ON apple_revocation_outbox (status, attempt_count, next_attempt_at);


