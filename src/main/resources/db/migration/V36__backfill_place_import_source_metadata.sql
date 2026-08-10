UPDATE place_imports place_import
    JOIN contents content ON content.id = place_import.content_id
SET place_import.source_author_name = content.source_author_name,
    place_import.source_author_username = content.source_author_username,
    place_import.source_published_on = content.source_published_on,
    place_import.like_count = content.like_count,
    place_import.comment_count = content.comment_count,
    place_import.engagement_checked_at = content.engagement_checked_at;
