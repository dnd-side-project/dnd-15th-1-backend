ALTER TABLE contents
    MODIFY publication_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

UPDATE contents c
SET c.publication_status = CASE
    WHEN EXISTS (
        SELECT 1 FROM content_places cp WHERE cp.content_id = c.id
    ) THEN 'PUBLIC'
    ELSE 'HIDDEN'
END
WHERE c.publication_status = 'PUBLIC';

CREATE INDEX idx_content_places_place_content
    ON content_places (place_id, content_id);
