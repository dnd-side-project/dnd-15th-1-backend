package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PublicContentView;
import kr.omong.dulpick.domain.place.application.PublicPlaceView;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PublicContentResponse(
        @Schema(description = "모든 사용자가 공유하는 공개 게시물 ID입니다.", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long contentId,
        @Schema(description = "추적 파라미터를 제거한 정규화 게시물 링크입니다.", example = "https://www.instagram.com/reel/example/", requiredMode = Schema.RequiredMode.REQUIRED)
        String canonicalUrl,
        @Schema(
                description = "공개 게시물의 원본 콘텐츠 유형입니다. INSTAGRAM_REEL 또는 INSTAGRAM_POST입니다.",
                allowableValues = {"INSTAGRAM_REEL", "INSTAGRAM_POST"},
                example = "INSTAGRAM_REEL",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        ContentSourceType sourceType,
        @Schema(description = "게시물 작성자 정보. 확인할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        AuthorResponse author,
        @Schema(description = "게시물 게시일. 확인할 수 없으면 null입니다.", example = "2026-08-16", format = "date", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDate publishedOn,
        @Schema(description = "게시물 좋아요·댓글 집계 정보. 확인할 수 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        EngagementResponse engagement,
        @Schema(description = "게시물 제목. 원본에 없으면 null입니다.", example = "서울 데이트 추천 코스", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String title,
        @Schema(description = "게시물 본문 또는 캡션. 원본에 없으면 null입니다.", example = "분위기 좋은 데이트 장소를 소개합니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String caption,
        @Schema(description = "게시물 대표 이미지 URL. 없으면 null입니다.", example = "https://example.com/thumbnail.jpg", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String thumbnailUrl,
        @Schema(description = "공개 게시물에 연결된 장소 수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        int placeCount,
        @Schema(description = "게시물에 연결된 장소 목록입니다. 장소가 없으면 빈 배열입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PublicPlaceResponse> places
) {

    public static PublicContentResponse from(PublicContentView view) {
        return new PublicContentResponse(
                view.contentId(),
                view.canonicalUrl(),
                view.sourceType(),
                AuthorResponse.from(view.author()),
                view.publishedOn(),
                EngagementResponse.from(view.engagement()),
                view.title(),
                view.caption(),
                view.thumbnailUrl(),
                view.placeCount(),
                view.places().stream().map(PublicPlaceResponse::from).toList()
        );
    }

    public record AuthorResponse(
            @Schema(description = "작성자에게 표시되는 이름. 없으면 null입니다.", example = "둘픽이", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String displayName,
            @Schema(description = "작성자 사용자명. 없으면 null입니다.", example = "dulpick_user", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String username
    ) {

        private static AuthorResponse from(PublicContentView.ContentAuthorView view) {
            return view == null ? null : new AuthorResponse(view.displayName(), view.username());
        }
    }

    public record EngagementResponse(
            @Schema(description = "좋아요 수. 집계할 수 없으면 null입니다.", example = "128", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Long likeCount,
            @Schema(description = "댓글 수. 집계할 수 없으면 null입니다.", example = "24", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Long commentCount,
            @Schema(description = "반응 수를 확인한 시각. 집계할 수 없으면 null입니다.", example = "2026-08-16T14:30:00", format = "date-time", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            LocalDateTime checkedAt
    ) {

        private static EngagementResponse from(PublicContentView.ContentEngagementView view) {
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

    public record PublicPlaceResponse(
            @Schema(description = "모든 콘텐츠와 회원 저장 목록이 공유하는 정규화 장소 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
            Long placeId,
            @Schema(description = "Kakao 장소 검색 결과의 고유 ID", example = "18699959", requiredMode = Schema.RequiredMode.REQUIRED)
            String kakaoPlaceId,
            @Schema(description = "Kakao에서 확인한 장소명", example = "서울숲 카페", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @Schema(description = "Kakao 지번 주소", example = "서울특별시 성동구 성수동1가 685-700", requiredMode = Schema.RequiredMode.REQUIRED)
            String address,
            @Schema(description = "Kakao 도로명 주소. 없으면 null입니다.", example = "서울특별시 성동구 서울숲2길 10", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String roadAddress,
            @Schema(description = "WGS84 기준 위도. 없으면 null입니다.", example = "37.5446", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            BigDecimal latitude,
            @Schema(description = "WGS84 기준 경도. 없으면 null입니다.", example = "127.0557", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            BigDecimal longitude,
            @Schema(description = "Kakao가 제공한 원본 카테고리 경로. 없으면 null입니다.", example = "음식점 > 카페", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String category,
            @Schema(
                    description = "둘픽 화면에서 사용하는 장소 분류입니다.",
                    allowableValues = {"맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"},
                    example = "카페",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String categoryName,
            @Schema(description = "현재 회원이 이미 저장한 장소이면 true입니다.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
            boolean savedByMe,
            @Schema(description = "대표 장소 이미지 URL. 없으면 null입니다.", example = "https://example.com/place.jpg", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String thumbnailUrl,
            @ArraySchema(schema = @Schema(example = "https://example.com/place-detail.jpg"))
            @Schema(description = "대표 이미지를 제외한 장소 이미지 URL 목록입니다. 없으면 빈 배열입니다.", example = "[\"https://example.com/place-detail.jpg\"]", requiredMode = Schema.RequiredMode.REQUIRED)
            List<String> imageUrls
    ) {

        private static PublicPlaceResponse from(PublicPlaceView view) {
            return new PublicPlaceResponse(
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
