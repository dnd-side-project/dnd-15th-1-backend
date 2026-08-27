-- 둘픽 스키마 V10: 이메일 공지.

CREATE TABLE email_opt_outs
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    category   VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_opt_outs_member_category
        UNIQUE (member_id, category),
    CONSTRAINT fk_email_opt_outs_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE email_announcements
(
    id             VARCHAR(36)  NOT NULL,
    category       VARCHAR(30)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    body           TEXT         NOT NULL,
    status         VARCHAR(40)  NOT NULL,
    target_count   INT          NOT NULL,
    delivery_mode  VARCHAR(20)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);
