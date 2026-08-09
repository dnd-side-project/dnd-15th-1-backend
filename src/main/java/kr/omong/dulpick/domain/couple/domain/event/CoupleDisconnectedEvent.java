package kr.omong.dulpick.domain.couple.domain.event;

import java.time.Instant;

public record CoupleDisconnectedEvent(
        Long coupleId,
        Long firstMemberId,
        Long secondMemberId,
        Long requestedByMemberId,
        Reason reason,
        Instant occurredAt
) {

    public enum Reason {
        USER_REQUEST,
        MEMBER_WITHDRAWAL
    }
}
