package kr.omong.dulpick.domain.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 255)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AnalyticsEventType eventType;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "couple_id")
    private Long coupleId;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AnalyticsEvent() {
    }

    private AnalyticsEvent(
            String eventKey,
            AnalyticsEventType eventType,
            Long memberId,
            Long coupleId,
            String resourceType,
            Long resourceId,
            Instant occurredAt
    ) {
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.memberId = memberId;
        this.coupleId = coupleId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.occurredAt = occurredAt;
        this.createdAt = occurredAt;
    }

    public static AnalyticsEvent record(
            String eventKey,
            AnalyticsEventType eventType,
            Long memberId,
            Long coupleId,
            String resourceType,
            Long resourceId,
            Instant occurredAt
    ) {
        if (eventKey == null || eventKey.isBlank() || eventType == null || occurredAt == null) {
            throw new IllegalArgumentException("Analytics event is invalid");
        }
        return new AnalyticsEvent(
                eventKey,
                eventType,
                memberId,
                coupleId,
                resourceType,
                resourceId,
                occurredAt
        );
    }
}
