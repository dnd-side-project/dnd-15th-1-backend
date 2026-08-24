ALTER TABLE content_images
    ADD COLUMN content_hash CHAR(64) NULL AFTER content_type;

CREATE INDEX idx_content_images_content_hash
    ON content_images (content_id, content_hash);
