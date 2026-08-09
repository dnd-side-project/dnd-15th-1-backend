ALTER TABLE place_imports
    ADD COLUMN title VARCHAR(1000),
    ADD COLUMN content TEXT,
    ADD COLUMN thumbnail_url VARCHAR(1000);
