package kr.omong.dulpick.domain.member.application.query.view;

import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.MemberStatus;

import java.time.Instant;
import java.util.List;

public record MemberProfileView(
        Long memberId,
        MemberStatus status,
        boolean onboardingCompleted,
        String nickname,
        Integer profileIcon,
        DatePreferences datePreferences,
        Instant createdAt,
        Instant updatedAt,
        Instant lastWithdrawnAt,
        Instant lastRejoinedAt,
        List<MemberSocialAccount> socialAccounts
) {
}
