package kr.omong.dulpick.domain.place.presentation.dto.response;

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
        @Schema(description = "모든 사용자가 공유하는 공개 게시물 ID")
        Long contentId,
        @Schema(description = "추적 파라미터를 제거한 정규화 게시물 링크")
        String canonicalUrl,
        ContentSourceType sourceType,
        AuthorResponse author,
        LocalDate publishedOn,
        EngagementResponse engagement,
        String title,
        String caption,
        String thumbnailUrl,
        int placeCount,
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

    public record AuthorResponse(String displayName, String username) {

        private static AuthorResponse from(PublicContentView.ContentAuthorView view) {
            return view == null ? null : new AuthorResponse(view.displayName(), view.username());
        }
    }

    public record EngagementResponse(
            Long likeCount,
            Long commentCount,
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
                    view.thumbnailUrl()
            );
        }
    }
}
