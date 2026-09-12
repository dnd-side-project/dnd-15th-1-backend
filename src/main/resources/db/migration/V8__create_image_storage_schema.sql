-- 둘픽 스키마 V8: 이미지 저장·재처리.

CREATE TABLE content_images
(
    image_key        CHAR(36)      NOT NULL,
    content_id       BIGINT        NOT NULL,
    source_url       VARCHAR(2000)   NOT NULL,
    source_url_hash  CHAR(64)      NOT NULL,
    storage_key      VARCHAR(255)  NOT NULL,
    content_type     VARCHAR(100)  NULL,
    display_order    INT           NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (image_key),
    CONSTRAINT uk_content_images_content_url
        UNIQUE (content_id, source_url_hash),
    CONSTRAINT fk_content_images_content
        FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);

CREATE INDEX idx_content_images_content_order
    ON content_images (content_id, display_order);

ALTER TABLE place_images
    ADD COLUMN storage_key VARCHAR(36) NULL,
    ADD COLUMN content_type VARCHAR(100) NULL;

CREATE UNIQUE INDEX uk_place_images_storage_key
    ON place_images (storage_key);
ALTER TABLE places
    ADD COLUMN dulpick_category_code VARCHAR(30) NULL AFTER category_group_code;

CREATE TABLE content_image_enrichment_backlogs
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    content_id        BIGINT       NOT NULL,
    source_urls       TEXT         NOT NULL,
    attempt_count     INT          NOT NULL DEFAULT 0,
    status            VARCHAR(20)  NOT NULL,
    next_attempt_at   DATETIME(6)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_image_enrichment_backlogs_content
        UNIQUE (content_id),
    CONSTRAINT fk_content_image_enrichment_backlogs_content
        FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);

CREATE INDEX idx_content_image_enrichment_backlogs_status_attempt
    ON content_image_enrichment_backlogs (status, next_attempt_at, id);
ALTER TABLE content_images
    ADD COLUMN content_hash CHAR(64) NULL AFTER content_type;

CREATE INDEX idx_content_images_content_hash
    ON content_images (content_id, content_hash);
