ALTER TABLE contents
    ADD COLUMN content_hash CHAR(64);

ALTER TABLE content_submissions
    DROP FOREIGN KEY fk_content_submissions_member,
    MODIFY member_id BIGINT NULL;
