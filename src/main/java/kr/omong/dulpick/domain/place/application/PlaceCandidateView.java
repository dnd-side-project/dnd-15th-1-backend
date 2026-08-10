package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;

import java.math.BigDecimal;
import java.util.List;

public record PlaceCandidateView(
        Long candidateId,
        PlaceVerificationStatus verificationStatus,
        String extractedName,
        String extractedAddressHint,
        VerifiedPlaceView place,
        String evidence,
        String mentionType
) {

    public record VerifiedPlaceView(
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

        public VerifiedPlaceView {
            imageUrls = imageUrls == null
                    ? List.of()
                    : imageUrls.stream()
                    .filter(imageUrl -> !imageUrl.equals(thumbnailUrl))
                    .distinct()
                    .toList();
        }

        public VerifiedPlaceView(
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
}
