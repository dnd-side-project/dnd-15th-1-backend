ALTER TABLE contents
    ADD COLUMN analysis_content_hash VARCHAR(64) NULL AFTER analyzed_at,
    ADD COLUMN analysis_status VARCHAR(30) NULL AFTER analysis_content_hash,
    ADD COLUMN analysis_started_at TIMESTAMP(6) NULL AFTER analysis_status;

CREATE INDEX idx_contents_analysis_claim
    ON contents (canonical_url_hash, content_hash, analysis_status, analysis_started_at);
