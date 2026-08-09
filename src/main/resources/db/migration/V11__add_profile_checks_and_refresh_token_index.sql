CREATE INDEX idx_refresh_tokens_member_revoked
    ON refresh_tokens (member_id, revoked_at);

ALTER TABLE member_profiles
    ADD CONSTRAINT chk_member_profiles_indoor_outdoor
        CHECK (indoor_outdoor IN ('INDOOR', 'OUTDOOR')),
    ADD CONSTRAINT chk_member_profiles_activity_level
        CHECK (activity_level IN ('ACTIVE', 'STATIC')),
    ADD CONSTRAINT chk_member_profiles_date_time
        CHECK (date_time IN ('DAY', 'NIGHT')),
    ADD CONSTRAINT chk_member_profiles_date_focus
        CHECK (date_focus IN ('FOOD', 'SIGHTSEEING'));
