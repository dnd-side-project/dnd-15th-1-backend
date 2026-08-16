ALTER TABLE member_profiles
    ADD CONSTRAINT ck_member_profiles_date_preferences_complete
        CHECK (
            (
                indoor_outdoor IS NULL
                AND activity_level IS NULL
                AND date_time IS NULL
                AND date_focus IS NULL
            )
            OR
            (
                indoor_outdoor IS NOT NULL
                AND activity_level IS NOT NULL
                AND date_time IS NOT NULL
                AND date_focus IS NOT NULL
            )
        );
