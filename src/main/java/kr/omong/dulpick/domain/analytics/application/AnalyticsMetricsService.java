package kr.omong.dulpick.domain.analytics.application;

import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventRepository;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType;
import kr.omong.dulpick.global.time.ServiceTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

@Service
public class AnalyticsMetricsService {

    private final AnalyticsEventRepository eventRepository;

    public AnalyticsMetricsService(AnalyticsEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsMetricsView overview(Instant from, Instant to) {
        long activeMembers = eventRepository.countDistinctMembersByTypes(
                List.of("PLACE_SAVED", "DATE_COURSE_CREATED"), from, to
        );
        long activeCouples = eventRepository.countDistinctCouplesByTypes(
                List.of("PLACE_SAVED", "DATE_COURSE_CREATED"), from, to
        );
        long newMembers = eventRepository.countMembersCreatedAt(from, to);
        long connectedCouples = eventRepository.countDistinctCouples(
                AnalyticsEventType.COUPLE_CONNECTED, from, to
        );
        long connectedMembers = eventRepository.countDistinctMembers(
                AnalyticsEventType.COUPLE_CONNECTED, from, to
        );
        long totalActiveMembers = eventRepository.countActiveMembers(to);
        long savedPlaces = count(AnalyticsEventType.PLACE_SAVED, from, to);
        long createdDateCourses = count(AnalyticsEventType.DATE_COURSE_CREATED, from, to);
        long sharedPlacesUsed = eventRepository.countDistinctSharedPlaceCouples(from, to);
        long coreActiveCouples = eventRepository.countCoreActiveCouples(from, to);
        long courseCouples = countDistinctCouples(AnalyticsEventType.DATE_COURSE_CREATED, from, to);
        long savedCouples = countDistinctCouples(AnalyticsEventType.PLACE_SAVED, from, to);
        long firstCoreMembers = eventRepository.countFirstCoreMembers(from, to);
        Instant idleSince = to.minusSeconds(30L * 24 * 60 * 60);
        return new AnalyticsMetricsView(
                from,
                to,
                count(AnalyticsEventType.DOWNLOAD_PAGE_VISITED, from, to),
                activeMembers,
                activeCouples,
                coreActiveCouples,
                newMembers,
                connectedCouples,
                connectedMembers,
                totalActiveMembers,
                savedPlaces,
                createdDateCourses,
                sharedPlacesUsed,
                ratio(eventRepository.countActivatedNewMembers(from, to), newMembers),
                ratio(connectedMembers, totalActiveMembers),
                ratio(courseCouples, activeCouples),
                ratio(eventRepository.countSaveToCourseCouples(from, to), savedCouples),
                ratio(eventRepository.countRepeatCourseCouples(from, to), courseCouples),
                ratio(eventRepository.countReturnedWithinFourteenDays(from, to), firstCoreMembers),
                ratio(eventRepository.countRetainedMembers(from, to, 7), firstCoreMembers),
                ratio(eventRepository.countRetainedMembers(from, to, 14), firstCoreMembers),
                ratio(eventRepository.countRetainedMembers(from, to, 28), firstCoreMembers),
                ratio(eventRepository.countIdleActiveMembers(idleSince, to),
                        totalActiveMembers),
                ratio(savedPlaces, activeMembers),
                ratio(savedPlaces, activeCouples),
                ratio(createdDateCourses, activeCouples)
        );
    }

    @Transactional(readOnly = true)
    public List<AnalyticsTrendView> trends(Instant from, Instant to) {
        Map<LocalDate, long[]> daily = new java.util.TreeMap<>();
        LocalDate first = from.atZone(ServiceTime.ZONE_ID).toLocalDate();
        LocalDate last = to.minusNanos(1).atZone(ServiceTime.ZONE_ID).toLocalDate();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            daily.put(date, new long[6]);
        }
        for (Object[] row : eventRepository.findDailyEventCounts(from, to)) {
            LocalDate date = toLocalDate(row[0]);
            int index = trendIndex((String) row[1]);
            if (index >= 0) {
                daily.computeIfAbsent(date, ignored -> new long[6])[index] = ((Number) row[2]).longValue();
            }
        }
        for (Object[] row : eventRepository.findDailyMemberSignupCounts(from, to)) {
            LocalDate date = toLocalDate(row[0]);
            daily.computeIfAbsent(date, ignored -> new long[6])[1] = ((Number) row[2]).longValue();
        }
        return daily.entrySet().stream()
                .map(entry -> new AnalyticsTrendView(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2],
                        entry.getValue()[3],
                        entry.getValue()[4],
                        entry.getValue()[5]
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalyticsFunnelView funnel(Instant from, Instant to) {
        List<AnalyticsFunnelView.Step> steps = new ArrayList<>();
        addFunnelStep(steps, "가입", eventRepository.countMembersCreatedAt(from, to));
        addFunnelStep(steps, "커플 연결", eventRepository.countDistinctMembers(
                AnalyticsEventType.COUPLE_CONNECTED, from, to));
        addFunnelStep(steps, "장소 조회", eventRepository.countDistinctMembers(
                AnalyticsEventType.PLACE_VIEWED, from, to));
        addFunnelStep(steps, "장소 저장", eventRepository.countDistinctMembers(
                AnalyticsEventType.PLACE_SAVED, from, to));
        addFunnelStep(steps, "데이트 코스 생성", eventRepository.countDistinctMembers(
                AnalyticsEventType.DATE_COURSE_CREATED, from, to));
        return new AnalyticsFunnelView(steps);
    }

    @Transactional(readOnly = true)
    public AnalyticsRetentionView retention(Instant from, Instant to) {
        long cohortSize = eventRepository.countFirstCoreMembers(from, to);
        return new AnalyticsRetentionView(List.of(
                retentionPeriod("W1", cohortSize, eventRepository.countRetainedMembers(from, to, 7)),
                retentionPeriod("W2", cohortSize, eventRepository.countRetainedMembers(from, to, 14)),
                retentionPeriod("W4", cohortSize, eventRepository.countRetainedMembers(from, to, 28))
        ));
    }

    private long count(AnalyticsEventType type, Instant from, Instant to) {
        return eventRepository.countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(type, from, to);
    }

    private long countDistinctCouples(AnalyticsEventType type, Instant from, Instant to) {
        return eventRepository.countDistinctCouples(type, from, to);
    }

    private Double ratio(long numerator, long denominator) {
        return denominator == 0 ? null : (double) numerator / denominator;
    }

    private void addFunnelStep(List<AnalyticsFunnelView.Step> steps, String name, long count) {
        long previous = steps.isEmpty() ? 0 : steps.getLast().count();
        Double conversion = steps.isEmpty() ? null : ratio(count, previous);
        steps.add(new AnalyticsFunnelView.Step(
                name,
                count,
                conversion,
                conversion == null ? null : Math.max(0, 1 - conversion)
        ));
    }

    private AnalyticsRetentionView.Period retentionPeriod(
            String name,
            long cohortSize,
            long retainedMembers
    ) {
        return new AnalyticsRetentionView.Period(
                name,
                cohortSize,
                retainedMembers,
                ratio(retainedMembers, cohortSize)
        );
    }

    private int trendIndex(String eventType) {
        return switch (eventType) {
            case "DOWNLOAD_PAGE_VISITED" -> 0;
            case "MEMBER_SIGNED_UP" -> 1;
            case "COUPLE_CONNECTED" -> 2;
            case "PLACE_VIEWED" -> 3;
            case "PLACE_SAVED" -> 4;
            case "DATE_COURSE_CREATED" -> 5;
            default -> -1;
        };
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }
}
