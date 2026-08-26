CREATE TABLE date_courses
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    couple_id            BIGINT       NOT NULL,
    created_by_member_id BIGINT       NOT NULL,
    title                VARCHAR(120) NOT NULL,
    scheduled_at         DATETIME(6)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_date_courses_status
        CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT fk_date_courses_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id),
    CONSTRAINT fk_date_courses_creator
        FOREIGN KEY (created_by_member_id) REFERENCES members (id)
);

CREATE INDEX idx_date_courses_couple_status_scheduled
    ON date_courses (couple_id, status, scheduled_at);

CREATE INDEX idx_date_courses_couple_scheduled
    ON date_courses (couple_id, scheduled_at);

CREATE TABLE date_course_places
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    date_course_id BIGINT      NOT NULL,
    place_id       BIGINT      NOT NULL,
    sequence_order INT         NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_date_course_places_course_order
        UNIQUE (date_course_id, sequence_order),
    CONSTRAINT uk_date_course_places_course_place
        UNIQUE (date_course_id, place_id),
    CONSTRAINT fk_date_course_places_course
        FOREIGN KEY (date_course_id) REFERENCES date_courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_date_course_places_place
        FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE INDEX idx_date_course_places_course_order
    ON date_course_places (date_course_id, sequence_order);
