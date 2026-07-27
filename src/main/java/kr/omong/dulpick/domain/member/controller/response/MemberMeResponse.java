package kr.omong.dulpick.domain.member.controller.response;

import kr.omong.dulpick.domain.member.application.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberStatus;

import java.time.Instant;
import java.util.List;

public record MemberMeResponse(
        Long memberId,
        MemberStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastWithdrawnAt,
        Instant lastRejoinedAt,
        List<MemberSocialAccountResponse> socialAccounts
) {

    public static MemberMeResponse from(MemberProfile profile) {
        List<MemberSocialAccountResponse> accounts = profile.socialAccounts()
                .stream()
                .map(MemberSocialAccountResponse::from)
                .toList();
        return new MemberMeResponse(
                profile.memberId(),
                profile.status(),
                profile.createdAt(),
                profile.updatedAt(),
                profile.lastWithdrawnAt(),
                profile.lastRejoinedAt(),
                accounts
        );
    }
}
