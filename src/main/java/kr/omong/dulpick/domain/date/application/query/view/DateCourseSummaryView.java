package kr.omong.dulpick.domain.date.application.query.view;

import kr.omong.dulpick.domain.date.domain.DateCourseStatus;

import java.time.Instant;

public record DateCourseSummaryView(
        Long dateCourseId,
        String title,
        Instant scheduledAt,
        DateCourseStatus status,
        long version,
        int totalPlaceCount
) {
}
