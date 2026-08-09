CREATE TABLE places
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    kakao_place_id  VARCHAR(80)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500) NOT NULL,
    road_address    VARCHAR(500),
    latitude        DECIMAL(10, 7),
    longitude       DECIMAL(10, 7),
    category        VARCHAR(100),
    thumbnail_url   VARCHAR(1000),
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_places_kakao_place_id UNIQUE (kakao_place_id)
);

CREATE TABLE place_imports
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    member_id             BIGINT       NOT NULL,
    canonical_url         VARCHAR(1000) NOT NULL,
    canonical_url_hash    CHAR(64)      NOT NULL,
    source_type            VARCHAR(30)  NOT NULL,
    content_hash          CHAR(64),
    source_updated_at     DATETIME(6),
    status                 VARCHAR(30)  NOT NULL,
    failure_code           VARCHAR(80),
    retry_count            INT          NOT NULL DEFAULT 0,
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    completed_at           DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_place_imports_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT uk_place_imports_member_url
        UNIQUE (member_id, canonical_url_hash)
);

CREATE TABLE place_candidates
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    import_id             BIGINT       NOT NULL,
    place_id              BIGINT,
    extracted_name        VARCHAR(255) NOT NULL,
    extracted_address_hint VARCHAR(500),
    verification_status   VARCHAR(30)  NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_place_candidates_import
        FOREIGN KEY (import_id) REFERENCES place_imports (id),
    CONSTRAINT fk_place_candidates_place
        FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE TABLE member_places
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    place_id         BIGINT       NOT NULL,
    source_import_id BIGINT,
    alias            VARCHAR(100),
    memo             VARCHAR(1000),
    saved_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_places_member_place UNIQUE (member_id, place_id),
    CONSTRAINT fk_member_places_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_member_places_place
        FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT fk_member_places_import
        FOREIGN KEY (source_import_id) REFERENCES place_imports (id)
);

CREATE INDEX idx_place_imports_member_created_at
    ON place_imports (member_id, created_at DESC);
CREATE INDEX idx_member_places_member_saved_at
    ON member_places (member_id, saved_at DESC);
