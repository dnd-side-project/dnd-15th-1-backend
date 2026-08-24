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
