ALTER TABLE place_imports
    ADD COLUMN source_author_name VARCHAR(255),
    ADD COLUMN source_author_username VARCHAR(255),
    ADD COLUMN source_published_on DATE,
    ADD COLUMN like_count BIGINT,
    ADD COLUMN comment_count BIGINT,
    ADD COLUMN engagement_checked_at DATETIME(6);
