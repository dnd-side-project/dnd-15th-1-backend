package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PlaceImportView(
        Long importId,
        Long contentId,
        String canonicalUrl,
        ContentSourceType sourceType,
        PlaceImportStatus status,
        PlaceImportNextAction nextAction,
        Long retryAfterSeconds,
        FailureView failure,
        ContentView content,
        Instant createdAt,
        Instant updatedAt,
        List<PlaceCandidateView> candidates
) {

    public record FailureView(
            String code,
            boolean retryable
    ) {
    }

    public record ContentView(
            String title,
            String caption,
            String thumbnailUrl,
            AuthorView author,
            LocalDate publishedOn,
            EngagementView engagement
    ) {
    }

    public record AuthorView(
            String displayName,
            String username
    ) {
    }

    public record EngagementView(
            Long likeCount,
            Long commentCount,
            Instant checkedAt
    ) {
    }
}
