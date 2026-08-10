package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;
import java.util.List;

public record VerifiedPlace(
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String categoryGroupCode,
        String category,
        String thumbnailUrl,
        List<String> imageUrls
) {

    public VerifiedPlace {
        imageUrls = imageUrls == null ? List.of() : imageUrls.stream().distinct().toList();
        if ((thumbnailUrl == null || thumbnailUrl.isBlank()) && !imageUrls.isEmpty()) {
            thumbnailUrl = imageUrls.getFirst();
            imageUrls = imageUrls.subList(1, imageUrls.size());
        } else {
            String selectedThumbnailUrl = thumbnailUrl;
            imageUrls = imageUrls.stream()
                    .filter(imageUrl -> !imageUrl.equals(selectedThumbnailUrl))
                    .toList();
        }
    }

    public VerifiedPlace(
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
                thumbnailUrl,
                List.of()
        );
    }
}
