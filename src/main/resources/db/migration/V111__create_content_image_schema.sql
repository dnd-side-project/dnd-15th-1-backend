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

INSERT INTO content_images
    (image_key, content_id, source_url, source_url_hash, storage_key,
     content_type, display_order, created_at, updated_at)
SELECT UUID(),
       content.id,
       content.thumbnail_url,
       SHA2(content.thumbnail_url, 256),
       CONCAT(UUID(), '.img'),
       NULL,
       0,
       content.created_at,
       content.updated_at
FROM contents content
WHERE content.thumbnail_url IS NOT NULL
  AND content.thumbnail_url <> ''
  AND content.source_type IN ('INSTAGRAM_REEL', 'INSTAGRAM_POST');
