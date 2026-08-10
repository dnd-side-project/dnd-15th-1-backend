package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceCandidateView;
import kr.omong.dulpick.domain.place.application.PlaceImportNextAction;
import kr.omong.dulpick.domain.place.application.PlaceImportView;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PlaceImportResponse(
        @Schema(description = "회원별 장소 분석 작업 ID. 상태 조회와 후보 확정 경로에 사용")
        Long importId,
        @Schema(
                description = "공용 게시물 ID. PUBLIC 전환 후 /api/v1/contents/{contentId}로 조회 가능",
                nullable = true
        )
        Long contentId,
        @Schema(description = "추적 파라미터를 제거한 정규화 게시물 링크")
        String canonicalUrl,
        @Schema(description = "콘텐츠 유형")
        ContentSourceType sourceType,
        @Schema(description = "분석 상태")
        PlaceImportStatus status,
        @Schema(description = "클라이언트의 다음 처리")
        PlaceImportNextAction nextAction,
        @Schema(description = "다음 상태 조회 또는 재시도까지 권장 대기 시간(초)", nullable = true)
        Long retryAfterSeconds,
        @Schema(description = "분석 실패 정보", nullable = true)
        FailureResponse failure,
        @Schema(description = "게시물 메타데이터")
        ContentResponse content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @Schema(description = "추출 또는 Kakao 검증된 장소 후보")
        List<PlaceCandidateResponse> candidates
) {

    public static PlaceImportResponse from(PlaceImportView view) {
        return new PlaceImportResponse(
                view.importId(),
                view.contentId(),
                view.canonicalUrl(),
                view.sourceType(),
                view.status(),
                view.nextAction(),
                view.retryAfterSeconds(),
                FailureResponse.from(view.failure()),
                ContentResponse.from(view.content()),
                ServiceTime.toLocalDateTime(view.createdAt()),
                ServiceTime.toLocalDateTime(view.updatedAt()),
                view.candidates().stream().map(PlaceCandidateResponse::from).toList()
        );
    }

    public record FailureResponse(
            String code,
            boolean retryable
    ) {

        private static FailureResponse from(PlaceImportView.FailureView view) {
            return view == null ? null : new FailureResponse(view.code(), view.retryable());
        }
    }

    public record ContentResponse(
            String title,
            String caption,
            String thumbnailUrl,
            @Schema(nullable = true)
            AuthorResponse author,
            LocalDate publishedOn,
            @Schema(nullable = true)
            EngagementResponse engagement
    ) {

        private static ContentResponse from(PlaceImportView.ContentView view) {
            return new ContentResponse(
                    view.title(),
                    view.caption(),
                    view.thumbnailUrl(),
                    AuthorResponse.from(view.author()),
                    view.publishedOn(),
                    EngagementResponse.from(view.engagement())
            );
        }
    }

    public record AuthorResponse(
            String displayName,
            String username
    ) {

        private static AuthorResponse from(PlaceImportView.AuthorView view) {
            return view == null ? null : new AuthorResponse(view.displayName(), view.username());
        }
    }

    public record EngagementResponse(
            Long likeCount,
            Long commentCount,
            LocalDateTime checkedAt
    ) {

        private static EngagementResponse from(PlaceImportView.EngagementView view) {
            if (view == null) {
                return null;
            }
            return new EngagementResponse(
                    view.likeCount(),
                    view.commentCount(),
                    ServiceTime.toLocalDateTime(view.checkedAt())
            );
        }
    }

    public record PlaceCandidateResponse(
            @Schema(description = "현재 importId에 속한 장소 후보 ID. confirm 요청에서 선택값으로 사용")
            Long candidateId,
            PlaceVerificationStatus verificationStatus,
            String extractedName,
            String extractedAddressHint,
            @Schema(description = "Kakao 검증 장소. EXTRACTED 상태는 null", nullable = true)
            VerifiedPlaceResponse place,
            String evidence,
            String mentionType
    ) {

        private static PlaceCandidateResponse from(PlaceCandidateView view) {
            return new PlaceCandidateResponse(
                    view.candidateId(),
                    view.verificationStatus(),
                    view.extractedName(),
                    view.extractedAddressHint(),
                    VerifiedPlaceResponse.from(view.place()),
                    view.evidence(),
                    view.mentionType()
            );
        }
    }

    public record VerifiedPlaceResponse(
            @Schema(description = "모든 콘텐츠와 회원 저장 목록이 공유하는 정규화 장소 ID")
            Long placeId,
            String kakaoPlaceId,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            @Schema(description = "Kakao가 제공한 원본 카테고리 경로")
            String category,
            @Schema(description = "둘픽 장소 분류", allowableValues = {
                    "맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"
            })
            String categoryName,
            @Schema(description = "현재 회원이 이미 저장한 장소이면 true")
            boolean savedByMe,
            String thumbnailUrl
    ) {

        private static VerifiedPlaceResponse from(PlaceCandidateView.VerifiedPlaceView view) {
            if (view == null) {
                return null;
            }
            return new VerifiedPlaceResponse(
                    view.placeId(),
                    view.kakaoPlaceId(),
                    view.name(),
                    view.address(),
                    view.roadAddress(),
                    view.latitude(),
                    view.longitude(),
                    view.category(),
                    view.categoryName(),
                    view.savedByMe(),
                    view.thumbnailUrl()
            );
        }
    }
}
