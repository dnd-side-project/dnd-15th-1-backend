package kr.omong.dulpick.domain.analytics.application;

import java.time.Instant;

public record AnalyticsMetricsView(
        Instant from,
        Instant to,
        long downloads,
        long activeMembers,
        long activeCouples,
        long coreActiveCouples,
        long newMembers,
        long connectedCouples,
        long savedPlaces,
        long createdDateCourses,
        long sharedPlacesUsedInCourses,
        Double activationRate,
        Double courseUsageRate,
        Double saveToCourseRate,
        Double repeatCourseRate,
        Double repeatUsageRate,
        Double w1RetentionRate,
        Double w2RetentionRate,
        Double w4RetentionRate,
        Double idleMemberRate
) {
}
