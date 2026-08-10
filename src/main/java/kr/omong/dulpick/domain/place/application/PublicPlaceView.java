package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;

public record PublicPlaceView(
        Long placeId,
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        String categoryName,
        boolean savedByMe,
        String thumbnailUrl
) {
}
