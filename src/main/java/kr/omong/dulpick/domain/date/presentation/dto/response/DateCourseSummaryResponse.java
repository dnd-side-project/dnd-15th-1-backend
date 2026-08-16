package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseSummaryView;
import kr.omong.dulpick.domain.date.domain.DateCourseStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DateCourseSummaryResponse(
        @Schema(example = "1001")
        Long dateCourseId,
        @Schema(example = "성수동 데이트")
        String title,
        @Schema(description = "데이트 날짜(Asia/Seoul)", example = "2026-08-16")
        LocalDate date,
        @Schema(description = "데이트 시간(Asia/Seoul)", example = "19:30:00")
        LocalTime time,
        @Schema(example = "CONFIRMED")
        DateCourseStatus status,
        @Schema(example = "0")
        long version,
        @Schema(description = "코스에 포함된 총 장소 수", example = "2")
        int totalPlaceCount
) {

    public static DateCourseSummaryResponse from(DateCourseSummaryView view) {
        LocalDateTime scheduledAt = ServiceTime.toLocalDateTime(view.scheduledAt());
        return new DateCourseSummaryResponse(
                view.dateCourseId(),
                view.title(),
                scheduledAt == null ? null : scheduledAt.toLocalDate(),
                scheduledAt == null ? null : scheduledAt.toLocalTime(),
                view.status(),
                view.version(),
                view.totalPlaceCount()
        );
    }
}
