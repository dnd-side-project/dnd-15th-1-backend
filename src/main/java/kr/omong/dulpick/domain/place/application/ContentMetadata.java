package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.time.Instant;
import java.time.LocalDate;

public record ContentMetadata(
        String canonicalUrl,
        ContentSourceType sourceType,
        String title,
        String caption,
        String thumbnailUrl,
        String contentHash,
        Instant sourceUpdatedAt,
        String sourceAuthorName,
        String sourceAuthorUsername,
        LocalDate sourcePublishedOn,
        Long likeCount,
        Long commentCount,
        Instant engagementCheckedAt
) {
}
