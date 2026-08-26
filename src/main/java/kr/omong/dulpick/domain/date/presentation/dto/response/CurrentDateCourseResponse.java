package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseSummaryView;

public record CurrentDateCourseResponse(
        @Schema(nullable = true, description = "가장 가까운 확정 데이트 일정. 없으면 null")
        DateCourseSummaryResponse currentDateCourse
) {

    public static CurrentDateCourseResponse from(DateCourseSummaryView summary) {
        return new CurrentDateCourseResponse(
                summary == null ? null : DateCourseSummaryResponse.from(summary)
        );
    }
}
