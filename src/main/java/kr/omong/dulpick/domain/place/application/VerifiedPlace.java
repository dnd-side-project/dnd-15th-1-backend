package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;

public record VerifiedPlace(
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
}
