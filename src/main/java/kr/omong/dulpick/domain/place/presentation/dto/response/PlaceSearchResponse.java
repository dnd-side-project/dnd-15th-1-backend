package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;

import java.math.BigDecimal;

public record PlaceSearchResponse(
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
        String categoryName
) {

    public static PlaceSearchResponse from(PlaceSearchResult result) {
        return new PlaceSearchResponse(
                result.kakaoPlaceId(),
                result.name(),
                result.address(),
                result.roadAddress(),
                result.latitude(),
                result.longitude(),
                result.category(),
                DulpickPlaceCategory.fromKakao(
                        result.categoryGroupCode(),
                        result.category()
                ).getDisplayName()
        );
    }
}
