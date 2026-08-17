package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
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
        @Schema(example = "1001")
        Long dateCourseId,
        @Schema(example = "성수동 데이트")
        String title,
        @Schema(description = "데이트 날짜(Asia/Seoul)", example = "2026-08-16")
        LocalDate date,
        @Schema(description = "데이트 시간(Asia/Seoul)", example = "19:30:00")
        LocalTime time,
        @Schema(example = "DRAFT")
        DateCourseStatus status,
        @Schema(example = "0")
        long version,
        @Schema(example = "2")
        int totalPlaceCount,
        @Schema(example = "[]")
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
            @Schema(description = "데이트 코스 내 장소 순서", example = "1")
            int order,
            @Schema(example = "101")
            Long placeId,
            @Schema(example = "서울숲 카페")
            String name,
            @Schema(example = "서울특별시 성동구 성수동1가 685-700")
            String address,
            @Schema(example = "서울특별시 성동구 서울숲2길 10")
            String roadAddress,
            @Schema(example = "37.5446")
            BigDecimal latitude,
            @Schema(example = "127.0557")
            BigDecimal longitude,
            @Schema(description = "Kakao 원본 카테고리 경로", example = "음식점 > 카페")
            String category,
            @Schema(description = "둘픽 장소 분류", example = "카페")
            String categoryName,
            @Schema(example = "https://example.com/place.jpg")
            String thumbnailUrl,
            @ArraySchema(schema = @Schema(example = "https://example.com/place-detail.jpg"))
            @Schema(example = "[]")
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
