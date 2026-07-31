ALTER TABLE social_accounts
    ADD COLUMN provider_client_id VARCHAR(255) NULL
        AFTER provider_refresh_token;
