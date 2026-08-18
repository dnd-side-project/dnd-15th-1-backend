CREATE TABLE place_classifications
(
    place_id                  BIGINT      NOT NULL,
    environment_type         VARCHAR(20) NULL,
    environment_source       VARCHAR(20) NULL,
    activity_type            VARCHAR(20) NULL,
    activity_source          VARCHAR(20) NULL,
    time_type                VARCHAR(20) NULL,
    time_source              VARCHAR(20) NULL,
    focus_type               VARCHAR(20) NULL,
    focus_source             VARCHAR(20) NULL,
    created_at               DATETIME(6) NOT NULL,
    updated_at               DATETIME(6) NOT NULL,
    PRIMARY KEY (place_id),
    CONSTRAINT fk_place_classifications_place
        FOREIGN KEY (place_id) REFERENCES places (id) ON DELETE CASCADE,
    CONSTRAINT ck_place_classifications_environment_value
        CHECK (environment_type IS NULL OR environment_type IN ('INDOOR', 'OUTDOOR')),
    CONSTRAINT ck_place_classifications_activity_value
        CHECK (activity_type IS NULL OR activity_type IN ('ACTIVE', 'STATIC')),
    CONSTRAINT ck_place_classifications_time_value
        CHECK (time_type IS NULL OR time_type IN ('DAY', 'NIGHT')),
    CONSTRAINT ck_place_classifications_focus_value
        CHECK (focus_type IS NULL OR focus_type IN ('FOOD', 'SIGHTSEEING')),
    CONSTRAINT ck_place_classifications_environment_pair
        CHECK ((environment_type IS NULL AND environment_source IS NULL)
            OR (environment_type IS NOT NULL AND environment_source IN ('AI', 'MANUAL'))),
    CONSTRAINT ck_place_classifications_activity_pair
        CHECK ((activity_type IS NULL AND activity_source IS NULL)
            OR (activity_type IS NOT NULL AND activity_source IN ('AI', 'MANUAL'))),
    CONSTRAINT ck_place_classifications_time_pair
        CHECK ((time_type IS NULL AND time_source IS NULL)
            OR (time_type IS NOT NULL AND time_source IN ('AI', 'MANUAL'))),
    CONSTRAINT ck_place_classifications_focus_pair
        CHECK ((focus_type IS NULL AND focus_source IS NULL)
            OR (focus_type IS NOT NULL AND focus_source IN ('AI', 'MANUAL')))
);
