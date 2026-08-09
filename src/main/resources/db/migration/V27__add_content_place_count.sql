ALTER TABLE contents
    ADD COLUMN place_count INT NOT NULL DEFAULT 0 AFTER content_hash;

UPDATE contents c
SET c.place_count = (
    SELECT COUNT(*)
    FROM content_places cp
    WHERE cp.content_id = c.id
);
