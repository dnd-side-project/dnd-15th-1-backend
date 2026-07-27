package kr.omong.dulpick.domain.member.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record MemberSocialAccount(
        SocialProvider provider,
        String email
) {
}
