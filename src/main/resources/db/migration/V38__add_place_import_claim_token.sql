ALTER TABLE place_imports
    ADD COLUMN processing_claim_token VARCHAR(36) NULL AFTER status;
