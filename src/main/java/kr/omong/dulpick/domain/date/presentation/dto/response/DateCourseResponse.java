package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceView;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseView;
import kr.omong.dulpick.domain.date.domain.DateCourseStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DateCourseResponse(
        Long dateCourseId,
        String title,
        @Schema(description = "데이트 날짜(Asia/Seoul)")
        LocalDate date,
        @Schema(description = "데이트 시간(Asia/Seoul)")
        LocalTime time,
        DateCourseStatus status,
        long version,
        int totalPlaceCount,
        List<DateCoursePlaceResponse> places
) {

    public static DateCourseResponse from(DateCourseView view) {
        LocalDateTime scheduledAt = ServiceTime.toLocalDateTime(view.scheduledAt());
        return new DateCourseResponse(
                view.dateCourseId(),
                view.title(),
                scheduledAt == null ? null : scheduledAt.toLocalDate(),
                scheduledAt == null ? null : scheduledAt.toLocalTime(),
                view.status(),
                view.version(),
                view.totalPlaceCount(),
                view.places().stream().map(DateCoursePlaceResponse::from).toList()
        );
    }

    public record DateCoursePlaceResponse(
            int order,
            Long placeId,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            @Schema(description = "Kakao 원본 카테고리 경로")
            String category,
            @Schema(description = "둘픽 장소 분류")
            String categoryName,
            String thumbnailUrl,
            List<String> imageUrls
    ) {

        private static DateCoursePlaceResponse from(DateCoursePlaceView view) {
            return new DateCoursePlaceResponse(
                    view.order(),
                    view.placeId(),
                    view.name(),
                    view.address(),
                    view.roadAddress(),
                    view.latitude(),
                    view.longitude(),
                    view.category(),
                    view.categoryName(),
                    view.thumbnailUrl(),
                    view.imageUrls()
            );
        }
    }
}
