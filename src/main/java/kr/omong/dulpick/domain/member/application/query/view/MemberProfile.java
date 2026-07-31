package kr.omong.dulpick.domain.member.application.query.view;

import kr.omong.dulpick.domain.member.domain.MemberStatus;

import java.time.Instant;
import java.util.List;

public record MemberProfile(
        Long memberId,
        MemberStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastWithdrawnAt,
        Instant lastRejoinedAt,
        List<MemberSocialAccount> socialAccounts
) {
}
