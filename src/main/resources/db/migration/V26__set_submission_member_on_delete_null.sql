UPDATE content_submissions submission
LEFT JOIN members member ON member.id = submission.member_id
SET submission.member_id = NULL
WHERE member.id IS NULL;

ALTER TABLE content_submissions
    ADD CONSTRAINT fk_content_submissions_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE SET NULL;
