package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseSummaryView;
import kr.omong.dulpick.domain.date.domain.DateCourseStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DateCourseSummaryResponse(
        Long dateCourseId,
        String title,
        @Schema(description = "데이트 날짜(Asia/Seoul)")
        LocalDate date,
        @Schema(description = "데이트 시간(Asia/Seoul)")
        LocalTime time,
        DateCourseStatus status,
        long version,
        @Schema(description = "코스에 포함된 총 장소 수")
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
