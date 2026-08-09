package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.time.Instant;

public record ContentMetadata(
        String canonicalUrl,
        ContentSourceType sourceType,
        String title,
        String caption,
        String contentHash,
        Instant sourceUpdatedAt
) {
}
