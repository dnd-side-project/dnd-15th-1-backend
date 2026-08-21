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
