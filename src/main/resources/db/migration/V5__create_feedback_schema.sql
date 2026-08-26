CREATE TABLE member_feedbacks
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    member_id         BIGINT        NOT NULL,
    client_request_id CHAR(36)      NOT NULL,
    type              VARCHAR(30)   NOT NULL,
    content           VARCHAR(1000) NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_feedbacks_member_request
        UNIQUE (member_id, client_request_id),
    CONSTRAINT ck_member_feedbacks_type
        CHECK (type IN ('INQUIRY', 'BUG_REPORT', 'FEATURE_SUGGESTION', 'OTHER')),
    CONSTRAINT ck_member_feedbacks_status
        CHECK (status IN ('RECEIVED', 'IN_REVIEW', 'RESOLVED')),
    CONSTRAINT fk_member_feedbacks_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_member_feedbacks_member_created
    ON member_feedbacks (member_id, created_at);
