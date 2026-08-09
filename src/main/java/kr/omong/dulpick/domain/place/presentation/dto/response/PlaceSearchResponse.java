package kr.omong.dulpick.domain.place.presentation.dto.response;

import kr.omong.dulpick.domain.place.application.PlaceSearchResult;

import java.math.BigDecimal;

public record PlaceSearchResponse(
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String category
) {

    public static PlaceSearchResponse from(PlaceSearchResult result) {
        return new PlaceSearchResponse(
                result.kakaoPlaceId(),
                result.name(),
                result.address(),
                result.roadAddress(),
                result.latitude(),
                result.longitude(),
                result.category()
        );
    }
}
