package kr.omong.dulpick.domain.member.presentation.dto.response;

import kr.omong.dulpick.domain.member.application.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberStatus;

import java.time.Instant;
import java.util.List;

public record MemberResponse(
        Long memberId,
        MemberStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastWithdrawnAt,
        Instant lastRejoinedAt,
        List<MemberSocialAccountResponse> socialAccounts
) {

    public static MemberResponse from(MemberProfile profile) {
        List<MemberSocialAccountResponse> accounts = profile.socialAccounts()
                .stream()
                .map(MemberSocialAccountResponse::from)
                .toList();
        return new MemberResponse(
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
