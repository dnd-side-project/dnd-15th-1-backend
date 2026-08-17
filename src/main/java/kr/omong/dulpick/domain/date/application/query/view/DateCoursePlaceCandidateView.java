package kr.omong.dulpick.domain.date.application.query.view;

import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DateCoursePlaceCandidateView(
        Long placeId,
        String name,
        String address,
        String roadAddress,
        String region,
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

    public DateCoursePlaceCandidateView {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
