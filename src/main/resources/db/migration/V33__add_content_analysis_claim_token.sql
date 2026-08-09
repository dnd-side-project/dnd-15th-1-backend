ALTER TABLE contents
    ADD COLUMN analysis_claim_token VARCHAR(36) NULL AFTER analysis_started_at;
