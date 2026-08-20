CREATE TABLE walking_route_cache
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    from_place_id    BIGINT         NOT NULL,
    to_place_id      BIGINT         NOT NULL,
    from_latitude    DECIMAL(10, 7) NOT NULL,
    from_longitude   DECIMAL(10, 7) NOT NULL,
    to_latitude      DECIMAL(10, 7) NOT NULL,
    to_longitude     DECIMAL(10, 7) NOT NULL,
    distance_meters  INT            NOT NULL,
    duration_seconds INT            NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_walking_route_cache_place_pair
        UNIQUE (from_place_id, to_place_id),
    CONSTRAINT fk_walking_route_cache_from_place
        FOREIGN KEY (from_place_id) REFERENCES places (id) ON DELETE CASCADE,
    CONSTRAINT fk_walking_route_cache_to_place
        FOREIGN KEY (to_place_id) REFERENCES places (id) ON DELETE CASCADE
);

CREATE INDEX idx_walking_route_cache_from_place
    ON walking_route_cache (from_place_id);
