ALTER TABLE walking_route_cache
    ADD COLUMN path_points               TEXT NULL,
    ADD COLUMN transit_distance_meters   INT  NULL,
    ADD COLUMN transit_duration_seconds  INT  NULL,
    ADD COLUMN transit_path_points       TEXT NULL;
