ALTER TABLE places
    ADD COLUMN phone           VARCHAR(50)   NULL AFTER category_group_code,
    ADD COLUMN kakao_place_url VARCHAR(1000) NULL AFTER phone;
