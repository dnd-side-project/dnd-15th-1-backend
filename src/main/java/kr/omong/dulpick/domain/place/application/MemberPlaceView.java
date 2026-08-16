package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        Instant savedAt,
        String thumbnailUrl,
        List<String> imageUrls
) {

    public MemberPlaceView {
        imageUrls = imageUrls == null
                ? List.of()
                : imageUrls.stream()
                .filter(imageUrl -> !imageUrl.equals(thumbnailUrl))
                .distinct()
                .toList();
    }

    public MemberPlaceView(
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
            Instant savedAt
    ) {
        this(
                memberId,
                placeId,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                category,
                categoryName,
                ownershipStatus,
                alias,
                savedAt,
                null,
                List.of()
        );
    }
}
