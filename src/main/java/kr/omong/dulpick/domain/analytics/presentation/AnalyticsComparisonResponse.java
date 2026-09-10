package kr.omong.dulpick.domain.analytics.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.analytics.application.AnalyticsMetricsView;

import java.time.Instant;
import java.util.List;

public record AnalyticsComparisonResponse(
        @Schema(description = "현재 조회 기간")
        Period currentPeriod,
        @Schema(description = "현재 기간과 동일한 길이의 직전 기간")
        Period previousPeriod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Metric> metrics
) {

    public static AnalyticsComparisonResponse from(
            AnalyticsMetricsView current,
            AnalyticsMetricsView previous
    ) {
        return new AnalyticsComparisonResponse(
                Period.from(current),
                Period.from(previous),
                List.of(
                        Metric.of("downloads", "앱 다운로드 유입", current.downloads(), previous.downloads()),
                        Metric.of("newMembers", "신규 회원", current.newMembers(), previous.newMembers()),
                        Metric.of("activeMembers", "활성 사용자", current.activeMembers(), previous.activeMembers()),
                        Metric.of("activeCouples", "활성 커플", current.activeCouples(), previous.activeCouples()),
                        Metric.of("coreActiveCouples", "핵심 활성 커플", current.coreActiveCouples(), previous.coreActiveCouples()),
                        Metric.of("connectedMembers", "연결된 회원", current.connectedMembers(), previous.connectedMembers()),
                        Metric.of("savedPlaces", "장소 저장", current.savedPlaces(), previous.savedPlaces()),
                        Metric.of("createdDateCourses", "데이트 코스 생성", current.createdDateCourses(), previous.createdDateCourses()),
                        Metric.of("activationRate", "신규 활성화율", current.activationRate(), previous.activationRate()),
                        Metric.of("connectionRate", "커플 연결률", current.connectionRate(), previous.connectionRate()),
                        Metric.of("courseUsageRate", "데이트 코스 이용률", current.courseUsageRate(), previous.courseUsageRate()),
                        Metric.of("saveToCourseRate", "장소 저장→코스 전환율", current.saveToCourseRate(), previous.saveToCourseRate()),
                        Metric.of("repeatCourseRate", "반복 코스 생성률", current.repeatCourseRate(), previous.repeatCourseRate()),
                        Metric.of("repeatUsageRate", "14일 반복 사용률", current.repeatUsageRate(), previous.repeatUsageRate()),
                        Metric.of("w1RetentionRate", "W1 리텐션", current.w1RetentionRate(), previous.w1RetentionRate()),
                        Metric.of("w2RetentionRate", "W2 리텐션", current.w2RetentionRate(), previous.w2RetentionRate()),
                        Metric.of("w4RetentionRate", "W4 리텐션", current.w4RetentionRate(), previous.w4RetentionRate()),
                        Metric.of("idleMemberRate", "유휴 사용자 비율", current.idleMemberRate(), previous.idleMemberRate()),
                        Metric.of("averagePlacesPerActiveMember", "사용자당 평균 장소 저장", current.averagePlacesPerActiveMember(), previous.averagePlacesPerActiveMember()),
                        Metric.of("averagePlacesPerActiveCouple", "활성 커플당 평균 장소 저장", current.averagePlacesPerActiveCouple(), previous.averagePlacesPerActiveCouple()),
                        Metric.of("averageCoursesPerActiveCouple", "활성 커플당 평균 코스 생성", current.averageCoursesPerActiveCouple(), previous.averageCoursesPerActiveCouple())
                )
        );
    }

    public record Period(
            @Schema(example = "2026-09-01T00:00:00Z")
            Instant from,
            @Schema(example = "2026-10-01T00:00:00Z")
            Instant to
    ) {

        private static Period from(AnalyticsMetricsView view) {
            return new Period(view.from(), view.to());
        }
    }

    public record Metric(
            @Schema(example = "newMembers")
            String key,
            @Schema(example = "신규 회원")
            String label,
            @Schema(example = "20", nullable = true)
            Double currentValue,
            @Schema(example = "15", nullable = true)
            Double previousValue,
            @Schema(example = "5", nullable = true)
            Double difference,
            @Schema(example = "0.3333", nullable = true)
            Double changeRate
    ) {

        private static Metric of(String key, String label, long current, long previous) {
            return of(key, label, (double) current, (double) previous);
        }

        private static Metric of(String key, String label, Double current, Double previous) {
            Double difference = current == null || previous == null ? null : current - previous;
            Double changeRate = difference == null
                    ? null
                    : previous == 0 ? (difference == 0 ? 0.0 : null) : difference / Math.abs(previous);
            return new Metric(key, label, current, previous, difference, changeRate);
        }
    }
}
