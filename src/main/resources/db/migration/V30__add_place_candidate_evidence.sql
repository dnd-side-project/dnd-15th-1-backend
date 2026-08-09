ALTER TABLE place_candidates
    ADD COLUMN evidence VARCHAR(1000),
    ADD COLUMN mention_type VARCHAR(40);
