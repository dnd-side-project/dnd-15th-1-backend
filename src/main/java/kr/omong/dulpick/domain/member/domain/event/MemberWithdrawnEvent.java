package kr.omong.dulpick.domain.member.domain.event;

import java.time.Instant;

public record MemberWithdrawnEvent(Long memberId, Instant withdrawnAt) {
}
