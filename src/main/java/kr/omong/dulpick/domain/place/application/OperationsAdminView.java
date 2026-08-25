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
            @Schema(example = "2001")
            Long contentId,
            @Schema(example = "INSTAGRAM_REEL")
            ContentSourceType sourceType,
            @Schema(example = "https://www.instagram.com/reel/example/")
            String canonicalUrl,
            @Schema(example = "서울 데이트 추천")
            String title,
            @Schema(example = "분위기 좋은 장소를 소개합니다.")
            String content,
            @Schema(example = "PUBLIC")
            ContentPublicationStatus publicationStatus,
            List<ContentImage> images,
            List<PlaceSummary> places,
            @Schema(example = "2026-08-24T10:00:00Z")
            Instant createdAt,
            @Schema(example = "2026-08-24T10:00:05Z")
            Instant updatedAt
    ) {
    }

    public record ContentImage(
            @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
            String imageKey,
            @Schema(example = "https://example.com/admin-image.jpg")
            String imageUrl,
            @Schema(example = "https://example.com/source-image.jpg")
            String sourceUrl,
            @Schema(example = "image/jpeg")
            String contentType,
            @Schema(example = "0")
            int displayOrder,
            @Schema(example = "true")
            boolean stored,
            @Schema(example = "false")
            boolean thumbnail
    ) {
    }

    public record PlaceSummary(
            @Schema(example = "101")
            Long placeId,
            @Schema(example = "1234567890")
            String kakaoPlaceId,
            @Schema(example = "서울숲 카페")
            String name,
            @Schema(example = "서울특별시 성동구 성수동")
            String address,
            @Schema(example = "서울특별시 성동구 서울숲2길 10")
            String roadAddress,
            @Schema(example = "음식점 > 카페")
            String category,
            @Schema(example = "CE7")
            String categoryGroupCode,
            @Schema(example = "02-1234-5678")
            String phone,
            @Schema(example = "https://place.map.kakao.com/1234567890")
            String kakaoPlaceUrl,
            @Schema(example = "https://example.com/place.jpg")
            String thumbnailUrl,
            @Schema(example = "2026-08-24T10:00:05Z")
            Instant updatedAt
    ) {
    }

    public record PlaceDetail(
            PlaceSummary place,
            List<PlaceImage> images
    ) {
    }

    public record PlaceImage(
            @Schema(example = "501")
            Long imageId,
            @Schema(example = "https://example.com/place-image.jpg")
            String imageUrl,
            @Schema(example = "image/jpeg")
            String contentType,
            @Schema(example = "0")
            int displayOrder,
            @Schema(example = "true")
            boolean stored,
            @Schema(example = "false")
            boolean thumbnail
    ) {
    }

    public record PlaceSearchPage(
            List<PlaceSummary> places,
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
