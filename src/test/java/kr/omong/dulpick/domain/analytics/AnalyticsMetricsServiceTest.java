package kr.omong.dulpick.domain.analytics;

import kr.omong.dulpick.domain.analytics.application.AnalyticsMetricsService;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventRepository;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType;
import kr.omong.dulpick.global.time.ServiceTime;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsMetricsServiceTest {

    private final AnalyticsEventRepository repository = mock(AnalyticsEventRepository.class);
    private final AnalyticsMetricsService service = new AnalyticsMetricsService(repository);

    @Test
    void calculatesConversionRatesAndUsesDistinctSharedPlaceCouples() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        when(repository.countDistinctMembersByTypes(any(), eq(from), eq(to))).thenReturn(5L);
        when(repository.countDistinctCouplesByTypes(any(), eq(from), eq(to))).thenReturn(4L);
        when(repository.countCoreActiveCouples(from, to)).thenReturn(2L);
        when(repository.countDistinctSharedPlaceCouples(from, to)).thenReturn(3L);
        when(repository.countActivatedNewMembers(from, to)).thenReturn(3L);
        when(repository.countSaveToCourseCouples(from, to)).thenReturn(2L);
        when(repository.countRepeatCourseCouples(from, to)).thenReturn(1L);
        when(repository.countFirstCoreMembers(from, to)).thenReturn(3L);
        when(repository.countReturnedWithinFourteenDays(from, to)).thenReturn(2L);
        when(repository.countIdleActiveMembers(any(), eq(to))).thenReturn(1L);
        when(repository.countActiveMembers(to)).thenReturn(2L);
        when(repository.countMembersCreatedAt(from, to)).thenReturn(3L);
        when(repository.countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                eq(AnalyticsEventType.DOWNLOAD_PAGE_VISITED), eq(from), eq(to)
        )).thenReturn(10L);
        when(repository.countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                eq(AnalyticsEventType.PLACE_SAVED), eq(from), eq(to)
        )).thenReturn(10L);
        when(repository.countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                eq(AnalyticsEventType.DATE_COURSE_CREATED), eq(from), eq(to)
        )).thenReturn(10L);
        when(repository.countDistinctCouples(any(AnalyticsEventType.class), eq(from), eq(to)))
                .thenReturn(4L);

        var result = service.overview(from, to);

        assertThat(result.downloads()).isEqualTo(10L);
        assertThat(result.newMembers()).isEqualTo(3L);
        assertThat(result.activeMembers()).isEqualTo(5L);
        assertThat(result.activeCouples()).isEqualTo(4L);
        assertThat(result.sharedPlacesUsedInCourses()).isEqualTo(3L);
        assertThat(result.activationRate()).isEqualTo(1.0);
        assertThat(result.courseUsageRate()).isEqualTo(1.0);
        assertThat(result.saveToCourseRate()).isEqualTo(0.5);
        assertThat(result.repeatCourseRate()).isEqualTo(0.25);
        assertThat(result.repeatUsageRate()).isEqualTo(2.0 / 3.0);
        assertThat(result.idleMemberRate()).isEqualTo(0.5);
    }

    @Test
    void buildsDailyTrendsAndIncludesDaysWithoutEvents() {
        Instant from = ServiceTime.toScheduledInstant(java.time.LocalDate.of(2026, 9, 1), null);
        Instant to = ServiceTime.toScheduledInstant(java.time.LocalDate.of(2026, 9, 4), null);
        when(repository.findDailyEventCounts(from, to)).thenReturn(List.of(
                new Object[]{Date.valueOf("2026-09-01"), "PLACE_SAVED", 3L},
                new Object[]{Date.valueOf("2026-09-03"), "DOWNLOAD_PAGE_VISITED", 2L}
        ));
        when(repository.findDailyMemberSignupCounts(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{Date.valueOf("2026-09-02"), "MEMBER_SIGNED_UP", 1L}
        ));

        var result = service.trends(from, to);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).savedPlaces()).isEqualTo(3L);
        assertThat(result.get(1).downloads()).isZero();
        assertThat(result.get(1).newMembers()).isEqualTo(1L);
        assertThat(result.get(2).downloads()).isEqualTo(2L);
    }

    @Test
    void buildsFunnelWithPreviousStepConversion() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        when(repository.countMembersCreatedAt(from, to)).thenReturn(10L);
        when(repository.countDistinctMembers(eq(AnalyticsEventType.COUPLE_CONNECTED), eq(from), eq(to)))
                .thenReturn(5L);
        when(repository.countDistinctMembers(eq(AnalyticsEventType.PLACE_VIEWED), eq(from), eq(to)))
                .thenReturn(4L);
        when(repository.countDistinctMembers(eq(AnalyticsEventType.PLACE_SAVED), eq(from), eq(to)))
                .thenReturn(2L);
        when(repository.countDistinctMembers(eq(AnalyticsEventType.DATE_COURSE_CREATED), eq(from), eq(to)))
                .thenReturn(1L);

        var result = service.funnel(from, to);

        assertThat(result.steps()).hasSize(5);
        assertThat(result.steps().get(0).conversionFromPrevious()).isNull();
        assertThat(result.steps().get(1).conversionFromPrevious()).isEqualTo(0.5);
        assertThat(result.steps().get(1).dropoutFromPrevious()).isEqualTo(0.5);
        assertThat(result.steps().get(4).count()).isEqualTo(1L);
    }

    @Test
    void buildsRetentionPeriodsWithNullRateForAnEmptyCohort() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        when(repository.countFirstCoreMembers(from, to)).thenReturn(0L);

        var result = service.retention(from, to);

        assertThat(result.periods()).hasSize(3);
        assertThat(result.periods().get(0).name()).isEqualTo("W1");
        assertThat(result.periods().get(0).rate()).isNull();
    }
}
