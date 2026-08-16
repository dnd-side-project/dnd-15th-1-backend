package kr.omong.dulpick.domain.date.application.query.view;

import kr.omong.dulpick.domain.date.domain.DateCourseStatus;

import java.time.Instant;
import java.util.List;

public record DateCourseView(
        Long dateCourseId,
        String title,
        Instant scheduledAt,
        DateCourseStatus status,
        long version,
        int totalPlaceCount,
        List<DateCoursePlaceView> places
) {

    public DateCourseView {
        places = places == null ? List.of() : List.copyOf(places);
    }
}
