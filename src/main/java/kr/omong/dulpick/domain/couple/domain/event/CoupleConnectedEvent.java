package kr.omong.dulpick.domain.couple.domain.event;

import java.time.Instant;

public record CoupleConnectedEvent(
        Long coupleId,
        Long firstMemberId,
        Long secondMemberId,
        Instant occurredAt
) {
}
