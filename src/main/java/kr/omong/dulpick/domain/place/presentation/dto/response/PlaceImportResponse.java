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
        @Schema(
                description = "회원별 장소 분석 작업 ID입니다. GET 상태 조회와 POST 후보 확정 요청의 경로에 사용합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "1001"
        )
        Long importId,
        @Schema(
                description = "공용 게시물 ID입니다. 공용 콘텐츠로 전환되지 않은 분석 작업에서는 null입니다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Long contentId,
        @Schema(
                description = "추적 파라미터를 제거한 정규화 게시물 링크입니다. 같은 콘텐츠 재요청을 식별할 때 사용합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String canonicalUrl,
        @Schema(description = "분석 대상 콘텐츠 유형입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        ContentSourceType sourceType,
        @Schema(description = "분석 작업의 현재 상태입니다. 상태에 따라 nextAction을 수행합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        PlaceImportStatus status,
        @Schema(description = "현재 상태에서 클라이언트가 수행할 다음 동작입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        PlaceImportNextAction nextAction,
        @Schema(
                description = "다음 상태 조회 또는 재시도까지 권장하는 대기 시간(초)입니다. 즉시 처리할 필요가 없으면 null입니다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Long retryAfterSeconds,
        @Schema(
                description = "분석 실패 정보입니다. 실패하지 않은 작업에서는 null입니다.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        FailureResponse failure,
        @Schema(description = "원본 게시물에서 수집한 제목·본문·작성자·반응 정보입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        ContentResponse content,
        @Schema(description = "분석 작업 생성 시각", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt,
        @Schema(description = "분석 작업이 마지막으로 변경된 시각", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime updatedAt,
        @Schema(description = "AI가 추출하고 Kakao 검증을 거친 장소 후보 목록입니다. 후보가 없으면 빈 배열입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
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
            @Schema(description = "분석 실패 원인 코드", requiredMode = Schema.RequiredMode.REQUIRED)
            String code,
            @Schema(description = "동일 요청을 재시도해도 되는 실패인지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
            boolean retryable
    ) {

        private static FailureResponse from(PlaceImportView.FailureView view) {
            return view == null ? null : new FailureResponse(view.code(), view.retryable());
        }
    }

    public record ContentResponse(
            @Schema(description = "게시물 제목. 원본에 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String title,
            @Schema(description = "게시물 본문 또는 캡션. 원본에 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String caption,
            @Schema(description = "게시물 대표 이미지 URL. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String thumbnailUrl,
            @Schema(description = "게시물 작성자 정보. 확인할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            AuthorResponse author,
            @Schema(description = "게시물 게시일. 확인할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            LocalDate publishedOn,
            @Schema(description = "게시물 좋아요·댓글 집계 정보. 확인할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
            @Schema(description = "작성자에게 표시되는 이름. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String displayName,
            @Schema(description = "작성자 사용자명. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String username
    ) {

        private static AuthorResponse from(PlaceImportView.AuthorView view) {
            return view == null ? null : new AuthorResponse(view.displayName(), view.username());
        }
    }

    public record EngagementResponse(
            @Schema(description = "좋아요 수. 집계할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Long likeCount,
            @Schema(description = "댓글 수. 집계할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Long commentCount,
            @Schema(description = "반응 수를 확인한 시각. 집계할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
            @Schema(
                    description = "현재 importId에 속한 장소 후보 ID입니다. 후보 확정 요청의 candidateId로 사용합니다.",
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    example = "101"
            )
            Long candidateId,
            @Schema(description = "후보의 분석·검증 상태입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
            PlaceVerificationStatus verificationStatus,
            @Schema(description = "AI가 추출한 장소명", requiredMode = Schema.RequiredMode.REQUIRED)
            String extractedName,
            @Schema(description = "AI가 추출한 주소 힌트. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String extractedAddressHint,
            @Schema(description = "Kakao 검증이 완료된 장소 정보입니다. EXTRACTED 상태이거나 검증에 실패하면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            VerifiedPlaceResponse place,
            @Schema(description = "장소로 판단한 근거 문구. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String evidence,
            @Schema(description = "장소가 콘텐츠에서 언급된 방식입니다. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
            @Schema(
                    description = "모든 콘텐츠와 회원 저장 목록이 공유하는 정규화 장소 ID입니다.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Long placeId,
            @Schema(description = "Kakao 장소 검색 결과의 고유 ID", requiredMode = Schema.RequiredMode.REQUIRED)
            String kakaoPlaceId,
            @Schema(description = "Kakao에서 확인한 장소명", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @Schema(description = "Kakao 지번 주소", requiredMode = Schema.RequiredMode.REQUIRED)
            String address,
            @Schema(description = "Kakao 도로명 주소. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String roadAddress,
            @Schema(description = "WGS84 기준 위도. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            BigDecimal latitude,
            @Schema(description = "WGS84 기준 경도. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            BigDecimal longitude,
            @Schema(description = "Kakao가 제공한 원본 카테고리 경로. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String category,
            @Schema(
                    description = "둘픽 화면에서 사용하는 장소 분류입니다.",
                    allowableValues = {"맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"},
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String categoryName,
            @Schema(description = "현재 회원이 이미 저장한 장소이면 true입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
            boolean savedByMe,
            @Schema(description = "대표 장소 이미지 URL. 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String thumbnailUrl,
            @Schema(description = "대표 이미지를 제외한 장소 이미지 URL 목록입니다. 없으면 빈 배열입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
            List<String> imageUrls
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
                    view.thumbnailUrl(),
                    view.imageUrls()
            );
        }
    }
}
