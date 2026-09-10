package kr.omong.dulpick.domain.analytics.application;

import java.time.LocalDate;

public record AnalyticsTrendView(
        LocalDate date,
        long downloads,
        long newMembers,
        long connectedCouples,
        long placeViews,
        long savedPlaces,
        long createdDateCourses
) {
}
