ALTER TABLE members
    ADD COLUMN last_withdrawn_at DATETIME(6) NULL,
    ADD COLUMN last_rejoined_at DATETIME(6) NULL;
