CREATE TABLE contents
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    canonical_url      VARCHAR(1000) NOT NULL,
    canonical_url_hash CHAR(64)      NOT NULL,
    source_type        VARCHAR(30)   NOT NULL,
    title              VARCHAR(1000),
    content            TEXT,
    thumbnail_url      VARCHAR(1000),
    publication_status VARCHAR(30)   NOT NULL DEFAULT 'PUBLIC',
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    last_checked_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_contents_url_hash UNIQUE (canonical_url_hash)
);

CREATE TABLE content_submissions
(
    id           BIGINT     NOT NULL AUTO_INCREMENT,
    content_id   BIGINT     NOT NULL,
    member_id    BIGINT     NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_submissions_member_content UNIQUE (content_id, member_id),
    CONSTRAINT fk_content_submissions_content FOREIGN KEY (content_id) REFERENCES contents (id),
    CONSTRAINT fk_content_submissions_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE content_places
(
    id         BIGINT     NOT NULL AUTO_INCREMENT,
    content_id BIGINT     NOT NULL,
    place_id   BIGINT     NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_places_content_place UNIQUE (content_id, place_id),
    CONSTRAINT fk_content_places_content FOREIGN KEY (content_id) REFERENCES contents (id),
    CONSTRAINT fk_content_places_place FOREIGN KEY (place_id) REFERENCES places (id)
);

ALTER TABLE place_imports
    ADD COLUMN content_id BIGINT,
    ADD CONSTRAINT fk_place_imports_content
        FOREIGN KEY (content_id) REFERENCES contents (id);

CREATE INDEX idx_contents_publication_created
    ON contents (publication_status, created_at DESC);
CREATE INDEX idx_content_places_content
    ON content_places (content_id, created_at DESC);
