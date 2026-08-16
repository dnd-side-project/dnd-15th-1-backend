package kr.omong.dulpick.domain.date.application.query.view;

import java.math.BigDecimal;
import java.util.List;

public record DateCoursePlaceView(
        int order,
        Long placeId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        String categoryName,
        String thumbnailUrl,
        List<String> imageUrls
) {

    public DateCoursePlaceView {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
