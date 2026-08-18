CREATE TABLE region_tags
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(50)  NOT NULL,
    display_order INT          NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_region_tags_name
        UNIQUE (name)
);

CREATE INDEX idx_region_tags_active_order
    ON region_tags (active, display_order, id);

CREATE TABLE place_region_tags
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    place_id      BIGINT      NOT NULL,
    region_tag_id BIGINT      NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_place_region_tags_place_tag
        UNIQUE (place_id, region_tag_id),
    CONSTRAINT fk_place_region_tags_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE,
    CONSTRAINT fk_place_region_tags_region_tag
        FOREIGN KEY (region_tag_id) REFERENCES region_tags (id) ON DELETE CASCADE
);

CREATE INDEX idx_place_region_tags_region_place
    ON place_region_tags (region_tag_id, place_id);

INSERT INTO region_tags (name, display_order, active, created_at, updated_at)
VALUES ('성수', 1, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       ('강남', 2, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       ('을지로', 3, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       ('홍대', 4, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       ('잠실', 5, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO place_region_tags (place_id, region_tag_id, created_at)
SELECT place.id, region_tag.id, CURRENT_TIMESTAMP(6)
FROM places place
         CROSS JOIN region_tags region_tag
WHERE REPLACE(
              CONCAT(COALESCE(place.address, ''), COALESCE(place.road_address, '')),
              ' ',
              ''
      ) LIKE CONCAT('%', REPLACE(region_tag.name, ' ', ''), '%');
