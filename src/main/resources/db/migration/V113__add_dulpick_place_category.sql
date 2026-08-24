ALTER TABLE places
    ADD COLUMN dulpick_category_code VARCHAR(30) NULL AFTER category_group_code;

UPDATE places
SET dulpick_category_code = 'SHOPPING',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE kakao_place_id IN (
    '778434745',
    '1750832247',
    '509375963',
    '1088822321',
    '450924977'
);
