package kr.omong.dulpick.domain.notification.application.event;

import java.time.Instant;

public record ContentSavedEvent(
        Long coupleId,
        Long saverMemberId,
        Long partnerMemberId,
        Long contentId,
        Instant occurredAt
) {
}
