package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;
import java.time.Instant;

public record MemberPlaceView(
        Long memberId,
        Long placeId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        String alias,
        String memo,
        Instant savedAt
) {
}
