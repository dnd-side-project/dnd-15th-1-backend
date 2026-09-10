package kr.omong.dulpick.domain.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO analytics_events
                (event_key, event_type, member_id, couple_id, resource_type, resource_id, occurred_at, created_at)
            VALUES (:eventKey, :eventType, :memberId, :coupleId, :resourceType, :resourceId, :occurredAt, :createdAt)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventKey") String eventKey,
            @Param("eventType") String eventType,
            @Param("memberId") Long memberId,
            @Param("coupleId") Long coupleId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("occurredAt") Instant occurredAt,
            @Param("createdAt") Instant createdAt
    );

    long countByEventTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            AnalyticsEventType eventType,
            Instant from,
            Instant to
    );

    @Query("""
            SELECT COUNT(DISTINCT event.memberId)
            FROM AnalyticsEvent event
            WHERE event.eventType = :eventType
              AND event.memberId IS NOT NULL
              AND event.occurredAt >= :from
              AND event.occurredAt < :to
            """)
    long countDistinctMembers(
            @Param("eventType") AnalyticsEventType eventType,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT COUNT(DISTINCT member_id)
            FROM analytics_events
            WHERE event_type IN (:eventTypes)
              AND member_id IS NOT NULL
              AND occurred_at >= :from
              AND occurred_at < :to
            """, nativeQuery = true)
    long countDistinctMembersByTypes(
            @Param("eventTypes") List<String> eventTypes,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(DISTINCT event.coupleId)
            FROM AnalyticsEvent event
            WHERE event.eventType = :eventType
              AND event.coupleId IS NOT NULL
              AND event.occurredAt >= :from
              AND event.occurredAt < :to
            """)
    long countDistinctCouples(
            @Param("eventType") AnalyticsEventType eventType,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT COUNT(DISTINCT couple_id)
            FROM analytics_events
            WHERE event_type IN (:eventTypes)
              AND couple_id IS NOT NULL
              AND occurred_at >= :from
              AND occurred_at < :to
            """, nativeQuery = true)
    long countDistinctCouplesByTypes(
            @Param("eventTypes") List<String> eventTypes,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT couple_id
                FROM analytics_events
                WHERE couple_id IS NOT NULL
                  AND occurred_at >= :from
                  AND occurred_at < :to
                  AND event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                GROUP BY couple_id
                HAVING SUM(event_type = 'PLACE_SAVED') >= 3
                   AND SUM(event_type = 'DATE_COURSE_CREATED') >= 1
            ) active_couples
            """, nativeQuery = true)
    long countCoreActiveCouples(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT couple_id
                FROM analytics_events
                WHERE couple_id IS NOT NULL
                  AND occurred_at >= :from
                  AND occurred_at < :to
                GROUP BY couple_id
                HAVING SUM(event_type = 'PLACE_SAVED') >= 1
                   AND SUM(event_type = 'DATE_COURSE_CREATED') >= 1
            ) converted_couples
            """, nativeQuery = true)
    long countSaveToCourseCouples(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT couple_id
                FROM analytics_events
                WHERE couple_id IS NOT NULL
                  AND event_type = 'DATE_COURSE_CREATED'
                  AND occurred_at >= :from
                  AND occurred_at < :to
                GROUP BY couple_id
                HAVING COUNT(*) >= 2
            ) repeat_couples
            """, nativeQuery = true)
    long countRepeatCourseCouples(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT couple_id)
            FROM analytics_events
            WHERE event_type = 'SHARED_PLACE_USED_IN_COURSE'
              AND couple_id IS NOT NULL
              AND occurred_at >= :from
              AND occurred_at < :to
            """, nativeQuery = true)
    long countDistinctSharedPlaceCouples(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT member.id)
            FROM members member
            JOIN member_places saved_place
              ON saved_place.member_id = member.id
             AND saved_place.saved_at >= :from
             AND saved_place.saved_at < :to
            WHERE member.created_at >= :from
              AND member.created_at < :to
            """, nativeQuery = true)
    long countActivatedNewMembers(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT first_event.member_id)
            FROM analytics_events first_event
            WHERE first_event.member_id IS NOT NULL
              AND first_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
              AND first_event.occurred_at >= :from
              AND first_event.occurred_at < :to
              AND NOT EXISTS (
                  SELECT 1
                  FROM analytics_events earlier_event
                  WHERE earlier_event.member_id = first_event.member_id
                    AND earlier_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                    AND earlier_event.occurred_at < first_event.occurred_at
              )
              AND EXISTS (
                  SELECT 1
                  FROM analytics_events later_event
                  WHERE later_event.member_id = first_event.member_id
                    AND later_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                    AND later_event.occurred_at >= TIMESTAMPADD(DAY, :days, first_event.occurred_at)
                    AND later_event.occurred_at < TIMESTAMPADD(DAY, :days + 7, first_event.occurred_at)
              )
            """, nativeQuery = true)
    long countRetainedMembers(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("days") int days
    );

    @Query(value = """
            SELECT COUNT(DISTINCT first_event.member_id)
            FROM analytics_events first_event
            WHERE first_event.member_id IS NOT NULL
              AND first_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
              AND first_event.occurred_at >= :from
              AND first_event.occurred_at < :to
              AND NOT EXISTS (
                  SELECT 1
                  FROM analytics_events earlier_event
                  WHERE earlier_event.member_id = first_event.member_id
                    AND earlier_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                    AND earlier_event.occurred_at < first_event.occurred_at
              )
            """, nativeQuery = true)
    long countFirstCoreMembers(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT first_event.member_id)
            FROM analytics_events first_event
            WHERE first_event.member_id IS NOT NULL
              AND first_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
              AND first_event.occurred_at >= :from
              AND first_event.occurred_at < :to
              AND NOT EXISTS (
                  SELECT 1
                  FROM analytics_events earlier_event
                  WHERE earlier_event.member_id = first_event.member_id
                    AND earlier_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                    AND earlier_event.occurred_at < first_event.occurred_at
              )
              AND EXISTS (
                  SELECT 1
                  FROM analytics_events later_event
                  WHERE later_event.member_id = first_event.member_id
                    AND later_event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                    AND later_event.occurred_at > first_event.occurred_at
                    AND later_event.occurred_at < TIMESTAMPADD(DAY, 15, first_event.occurred_at)
              )
            """, nativeQuery = true)
    long countReturnedWithinFourteenDays(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM members member
            WHERE member.status = 'ACTIVE'
              AND member.created_at < :to
              AND NOT EXISTS (
                  SELECT 1
                  FROM analytics_events event
                  WHERE event.member_id = member.id
                    AND event.event_type IN ('PLACE_SAVED', 'DATE_COURSE_CREATED')
                    AND event.occurred_at >= :since
                    AND event.occurred_at < :to
              )
            """, nativeQuery = true)
    long countIdleActiveMembers(
            @Param("since") Instant since,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM members
            WHERE status = 'ACTIVE'
              AND created_at < :to
            """, nativeQuery = true)
    long countActiveMembers(@Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(*)
            FROM members
            WHERE created_at >= :from
              AND created_at < :to
            """, nativeQuery = true)
    long countMembersCreatedAt(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT DATE(occurred_at) AS occurred_date, event_type, COUNT(*) AS event_count
            FROM analytics_events
            WHERE occurred_at >= :from
              AND occurred_at < :to
              AND event_type <> 'MEMBER_SIGNED_UP'
            GROUP BY DATE(occurred_at), event_type
            ORDER BY occurred_date ASC, event_type ASC
            """, nativeQuery = true)
    List<Object[]> findDailyEventCounts(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT DATE(created_at) AS created_date, 'MEMBER_SIGNED_UP' AS event_type, COUNT(*) AS member_count
            FROM members
            WHERE created_at >= :from
              AND created_at < :to
            GROUP BY DATE(created_at)
            ORDER BY created_date ASC
            """, nativeQuery = true)
    List<Object[]> findDailyMemberSignupCounts(
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
