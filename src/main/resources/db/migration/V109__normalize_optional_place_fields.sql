UPDATE places
SET road_address = NULL
WHERE road_address IS NOT NULL
  AND TRIM(road_address) = '';

UPDATE member_places
SET alias = NULL
WHERE alias IS NOT NULL
  AND TRIM(alias) = '';
