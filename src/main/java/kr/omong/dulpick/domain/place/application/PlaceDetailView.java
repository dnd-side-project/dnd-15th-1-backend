package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;

import java.math.BigDecimal;
import java.util.List;

public record PlaceDetailView(
        Long placeId,
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        DulpickPlaceCategory categoryCode,
        String phone,
        String kakaoPlaceUrl,
        boolean savedByMe,
        PlaceOwnershipStatus ownershipStatus,
        String thumbnailUrl,
        List<String> imageUrls,
        List<RegionTagSummaryView> regionTags
) {

    public PlaceDetailView {
        imageUrls = imageUrls == null ? List.of() : imageUrls.stream().distinct().toList();
        regionTags = regionTags == null ? List.of() : List.copyOf(regionTags);
    }
}
