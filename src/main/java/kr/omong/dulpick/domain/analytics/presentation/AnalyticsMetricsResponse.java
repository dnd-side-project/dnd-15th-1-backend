package kr.omong.dulpick.domain.analytics.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.analytics.application.AnalyticsMetricsView;

import java.time.Instant;

public record AnalyticsMetricsResponse(
        @Schema(example = "2026-09-01T00:00:00Z")
        Instant from,
        @Schema(example = "2026-10-01T00:00:00Z")
        Instant to,
        @Schema(example = "150")
        long downloads,
        @Schema(example = "80")
        long activeMembers,
        @Schema(example = "40")
        long activeCouples,
        @Schema(example = "10")
        long coreActiveCouples,
        @Schema(example = "20")
        long newMembers,
        @Schema(example = "12")
        long connectedCouples,
        @Schema(example = "24")
        long connectedMembers,
        @Schema(example = "80")
        long totalActiveMembers,
        @Schema(example = "90")
        long savedPlaces,
        @Schema(example = "18")
        long createdDateCourses,
        @Schema(example = "8")
        long sharedPlacesUsedInCourses,
        @Schema(example = "0.5")
        Double activationRate,
        @Schema(example = "0.3", nullable = true)
        Double connectionRate,
        @Schema(example = "0.3")
        Double courseUsageRate,
        @Schema(example = "0.3")
        Double saveToCourseRate,
        @Schema(example = "0.2")
        Double repeatCourseRate,
        @Schema(example = "0.4")
        Double repeatUsageRate,
        @Schema(example = "0.25")
        Double w1RetentionRate,
        @Schema(example = "0.2")
        Double w2RetentionRate,
        @Schema(example = "0.1")
        Double w4RetentionRate,
        @Schema(example = "0.35")
        Double idleMemberRate,
        @Schema(example = "1.1", nullable = true)
        Double averagePlacesPerActiveMember,
        @Schema(example = "2.4", nullable = true)
        Double averagePlacesPerActiveCouple,
        @Schema(example = "0.5", nullable = true)
        Double averageCoursesPerActiveCouple
) {

    public static AnalyticsMetricsResponse from(AnalyticsMetricsView view) {
        return new AnalyticsMetricsResponse(
                view.from(),
                view.to(),
                view.downloads(),
                view.activeMembers(),
                view.activeCouples(),
                view.coreActiveCouples(),
                view.newMembers(),
                view.connectedCouples(),
                view.connectedMembers(),
                view.totalActiveMembers(),
                view.savedPlaces(),
                view.createdDateCourses(),
                view.sharedPlacesUsedInCourses(),
                view.activationRate(),
                view.connectionRate(),
                view.courseUsageRate(),
                view.saveToCourseRate(),
                view.repeatCourseRate(),
                view.repeatUsageRate(),
                view.w1RetentionRate(),
                view.w2RetentionRate(),
                view.w4RetentionRate(),
                view.idleMemberRate(),
                view.averagePlacesPerActiveMember(),
                view.averagePlacesPerActiveCouple(),
                view.averageCoursesPerActiveCouple()
        );
    }
}
