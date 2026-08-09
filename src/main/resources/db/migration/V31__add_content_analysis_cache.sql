ALTER TABLE contents
    ADD COLUMN analyzer_model VARCHAR(100),
    ADD COLUMN prompt_version VARCHAR(50),
    ADD COLUMN extracted_candidates_json TEXT,
    ADD COLUMN analyzed_at DATETIME(6);
