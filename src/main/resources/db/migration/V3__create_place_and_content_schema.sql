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
