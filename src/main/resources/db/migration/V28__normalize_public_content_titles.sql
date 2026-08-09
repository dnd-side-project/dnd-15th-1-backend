UPDATE contents
SET title = LEFT(
        TRIM(
            TRIM(BOTH '"' FROM
                SUBSTRING_INDEX(SUBSTRING_INDEX(title, ': "', -1), CHAR(10), 1)
            )
        ),
        200
    )
WHERE source_type IN ('INSTAGRAM_REEL', 'INSTAGRAM_POST')
  AND title LIKE '% on Instagram: "%';
