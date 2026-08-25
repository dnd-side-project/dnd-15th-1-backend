package kr.omong.dulpick.domain.place.application;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OperationsAdminView {

    private OperationsAdminView() {
    }

    public record Dashboard(
            @Schema(example = "42")
            long importsLast24Hours,
            @Schema(example = "20")
            long completedLast24Hours,
            @Schema(example = "15")
            long reviewRequiredLast24Hours,
            @Schema(example = "7")
            long failedLast24Hours,
            @Schema(example = "1")
            long staleProcessing,
            @Schema(example = "3")
            long pendingContents,
            @Schema(example = "2")
            long contentImageBacklogs,
            @Schema(example = "1")
            long placeImageBacklogs,
            @Schema(example = "4200")
            long averageDurationMs,
            @Schema(example = "12000")
            long maxDurationMs,
            Map<String, Long> failures,
            Map<String, Long> sources
    ) {
    }

    public record ImportPage(
            List<ImportSummary> imports,
            @Schema(example = "0")
            int page,
            @Schema(example = "20")
            int size,
            @Schema(example = "42")
            long totalElements,
            @Schema(example = "3")
            int totalPages,
            @Schema(example = "true")
            boolean hasNext
    ) {
    }

    public record ImportSummary(
            @Schema(example = "1001")
            Long importId,
            @Schema(example = "2001", nullable = true)
            Long contentId,
            @Schema(example = "INSTAGRAM_REEL")
            ContentSourceType sourceType,
            @Schema(example = "https://www.instagram.com/reel/example/")
            String canonicalUrl,
            @Schema(example = "REVIEW_REQUIRED")
            PlaceImportStatus status,
            @Schema(example = "PLACE_NOT_VERIFIED", nullable = true)
            String failureCode,
            @Schema(example = "1")
            int retryCount,
            @Schema(example = "2026-08-24T10:00:00Z")
            Instant createdAt,
            @Schema(example = "2026-08-24T10:00:05Z")
            Instant updatedAt,
            @Schema(example = "2026-08-24T10:00:05Z", nullable = true)
            Instant completedAt
    ) {
    }

    public record ImportDetail(
            ImportSummary summary,
            @Schema(example = "서울 데이트 추천")
            String title,
            @Schema(example = "분위기 좋은 장소를 소개합니다.", nullable = true)
            String caption,
            @Schema(example = "https://example.com/thumbnail.jpg", nullable = true)
            String thumbnailUrl,
            List<Candidate> candidates
    ) {
    }

    public record Candidate(
            @Schema(example = "3001")
            Long candidateId,
            @Schema(example = "서울숲 카페")
            String extractedName,
            @Schema(example = "성동구", nullable = true)
            String addressHint,
            @Schema(example = "VERIFIED")
            String verificationStatus,
            @Schema(example = "101", nullable = true)
            Long placeId,
            @Schema(example = "서울숲 카페", nullable = true)
            String placeName,
            @Schema(example = "서울특별시 성동구 성수이로", nullable = true)
            String placeAddress
    ) {
    }

    public record ContentPage(
            List<ContentSummary> contents,
            @Schema(example = "0")
            int page,
            @Schema(example = "20")
            int size,
            @Schema(example = "42")
            long totalElements,
            @Schema(example = "3")
            int totalPages,
            @Schema(example = "true")
            boolean hasNext
    ) {
    }

    public record ContentSummary(
            @Schema(example = "2001")
            Long contentId,
            @Schema(example = "INSTAGRAM_POST")
            ContentSourceType sourceType,
            @Schema(example = "https://www.instagram.com/p/example/")
            String canonicalUrl,
            @Schema(example = "서울 데이트 추천")
            String title,
            @Schema(example = "PUBLIC")
            ContentPublicationStatus publicationStatus,
            @Schema(example = "3")
            int placeCount,
            @Schema(example = "https://example.com/thumbnail.jpg", nullable = true)
            String thumbnailUrl,
            @Schema(example = "2026-08-24T10:00:00Z")
            Instant createdAt,
            @Schema(example = "2026-08-24T10:00:05Z")
            Instant updatedAt
        ) {
    }

    public record ContentDetail(
            Long contentId,
            ContentSourceType sourceType,
            String canonicalUrl,
            String title,
            String content,
            ContentPublicationStatus publicationStatus,
            List<ContentImage> images,
            List<PlaceSummary> places,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ContentImage(
            String imageKey,
            String imageUrl,
            String sourceUrl,
            String contentType,
            int displayOrder
    ) {
    }

    public record PlaceSummary(
            Long placeId,
            String kakaoPlaceId,
            String name,
            String address,
            String roadAddress,
            String category,
            String categoryGroupCode,
            String phone,
            String kakaoPlaceUrl,
            String thumbnailUrl
    ) {
    }

    public record PlaceDetail(
            PlaceSummary place,
            List<PlaceImage> images
    ) {
    }

    public record PlaceImage(
            Long imageId,
            String imageUrl,
            String contentType,
            int displayOrder
    ) {
    }

    public record PlaceSearchPage(
            List<PlaceSummary> places,
            int page,
            int size,
            boolean hasNext
    ) {
    }

    public record ImageBacklogPage(
            List<ImageBacklog> backlogs,
            @Schema(example = "0")
            int page,
            @Schema(example = "20")
            int size,
            @Schema(example = "4")
            long totalElements,
            @Schema(example = "1")
            int totalPages,
            @Schema(example = "false")
            boolean hasNext
    ) {
    }

    public record ImageBacklog(
            @Schema(example = "CONTENT")
            String kind,
            @Schema(example = "501")
            Long resourceId,
            @Schema(example = "2001", nullable = true)
            Long contentId,
            @Schema(example = "101", nullable = true)
            Long placeId,
            @Schema(example = "PENDING")
            String status,
            @Schema(example = "HTTP_403")
            String reason,
            @Schema(example = "2")
            int attemptCount,
            @Schema(example = "2026-08-24T10:00:00Z")
            Instant lastAttemptAt
    ) {
    }
}
