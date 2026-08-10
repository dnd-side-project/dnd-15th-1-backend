package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;
import java.util.List;

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
        String thumbnailUrl,
        List<String> imageUrls
) {

    public PublicPlaceView {
        imageUrls = imageUrls == null
                ? List.of()
                : imageUrls.stream()
                .filter(imageUrl -> !imageUrl.equals(thumbnailUrl))
                .distinct()
                .toList();
    }

    public PublicPlaceView(
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
        this(
                placeId,
                kakaoPlaceId,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                category,
                categoryName,
                savedByMe,
                thumbnailUrl,
                List.of()
        );
    }
}
