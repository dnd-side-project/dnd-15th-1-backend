-- 둘픽 스키마 V5: 회원 프로필 제약.

ALTER TABLE member_profiles
    MODIFY COLUMN indoor_outdoor VARCHAR(20) NULL,
    MODIFY COLUMN activity_level VARCHAR(20) NULL,
    MODIFY COLUMN date_time VARCHAR(20) NULL,
    MODIFY COLUMN date_focus VARCHAR(20) NULL;

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
