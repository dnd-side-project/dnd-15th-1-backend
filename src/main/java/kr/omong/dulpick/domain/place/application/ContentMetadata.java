package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        Instant engagementCheckedAt,
        List<String> imageUrls
) {

    public ContentMetadata(
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
        this(
                canonicalUrl,
                sourceType,
                title,
                caption,
                thumbnailUrl,
                contentHash,
                sourceUpdatedAt,
                sourceAuthorName,
                sourceAuthorUsername,
                sourcePublishedOn,
                likeCount,
                commentCount,
                engagementCheckedAt,
                thumbnailUrl == null || thumbnailUrl.isBlank() ? List.of() : List.of(thumbnailUrl)
        );
    }

    public ContentMetadata {
        imageUrls = imageUrls == null
                ? List.of()
                : imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
    }
}
