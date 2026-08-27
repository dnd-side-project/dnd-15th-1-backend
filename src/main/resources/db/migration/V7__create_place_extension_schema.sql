-- 둘픽 스키마 V7: 장소 확장·분류·경로·백로그.

ALTER TABLE places
    ADD COLUMN phone           VARCHAR(50)   NULL AFTER category_group_code,
    ADD COLUMN kakao_place_url VARCHAR(1000) NULL AFTER phone;

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

ALTER TABLE contents
    ADD FULLTEXT INDEX ft_contents_title_content (title, content) WITH PARSER ngram;

ALTER TABLE places
    ADD FULLTEXT INDEX ft_places_name_address (name, address, road_address) WITH PARSER ngram;

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

ALTER TABLE apple_revocation_outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        AFTER attempt_count;

CREATE INDEX idx_apple_revocation_outbox_status_attempt
    ON apple_revocation_outbox (status, attempt_count, next_attempt_at);

