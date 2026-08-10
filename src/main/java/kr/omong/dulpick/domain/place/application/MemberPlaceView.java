package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;

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
        String categoryName,
        PlaceOwnershipStatus ownershipStatus,
        String alias,
        String memo,
        Instant savedAt
) {
}
