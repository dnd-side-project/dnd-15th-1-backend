CREATE TABLE recent_searches
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    member_id        BIGINT       NOT NULL,
    search_type      VARCHAR(20)  NOT NULL,
    keyword          VARCHAR(200) NOT NULL,
    normalized_query VARCHAR(200) NOT NULL,
    searched_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recent_searches_member_type_query
        UNIQUE (member_id, search_type, normalized_query),
    CONSTRAINT fk_recent_searches_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT ck_recent_searches_type
        CHECK (search_type IN ('CONTENT', 'PLACE'))
);

CREATE INDEX idx_recent_searches_member_type_searched
    ON recent_searches (member_id, search_type, searched_at DESC, id DESC);
