ALTER TABLE place_images
    ADD COLUMN storage_key VARCHAR(36) NULL,
    ADD COLUMN content_type VARCHAR(100) NULL;

CREATE UNIQUE INDEX uk_place_images_storage_key
    ON place_images (storage_key);
