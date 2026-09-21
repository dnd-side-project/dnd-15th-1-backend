package kr.omong.dulpick.domain.analytics.domain;

import java.time.Instant;

public record AnalyticsActionEvent(
        String eventKey,
        AnalyticsEventType eventType,
        Long memberId,
        Long coupleId,
        String resourceType,
        Long resourceId,
        Instant occurredAt
) {
}
