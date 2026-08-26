package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;

public record PlaceSearchResult(
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String categoryGroupCode,
        String category,
        String phone,
        String kakaoPlaceUrl,
        String thumbnailUrl
) {

    public PlaceSearchResult(
            String kakaoPlaceId,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String categoryGroupCode,
            String category,
            String thumbnailUrl
    ) {
        this(
                kakaoPlaceId,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                categoryGroupCode,
                category,
                null,
                null,
                thumbnailUrl
        );
    }
}
