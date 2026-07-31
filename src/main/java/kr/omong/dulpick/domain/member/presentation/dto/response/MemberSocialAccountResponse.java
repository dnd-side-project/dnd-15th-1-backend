package kr.omong.dulpick.domain.member.presentation.dto.response;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.MemberSocialAccount;

public record MemberSocialAccountResponse(
        SocialProvider provider,
        String email
) {

    public static MemberSocialAccountResponse from(MemberSocialAccount account) {
        return new MemberSocialAccountResponse(account.provider(), account.email());
    }
}
