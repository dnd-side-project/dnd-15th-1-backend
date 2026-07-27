package kr.omong.dulpick.domain.member.controller.response;

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
