package kr.omong.dulpick.domain.notification.application.event;

import java.time.Instant;

public record DateCoursePlannedEvent(
        Long dateCourseId,
        Long coupleId,
        Long plannerMemberId,
        Long partnerMemberId,
        String plannerNickname,
        String dateCourseTitle,
        Instant occurredAt
) {
}
