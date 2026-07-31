CREATE INDEX idx_login_nonces_expires_at
    ON login_nonces (expires_at);

CREATE INDEX idx_login_nonces_used_at
    ON login_nonces (used_at);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

CREATE INDEX idx_refresh_tokens_revoked_at
    ON refresh_tokens (revoked_at);
