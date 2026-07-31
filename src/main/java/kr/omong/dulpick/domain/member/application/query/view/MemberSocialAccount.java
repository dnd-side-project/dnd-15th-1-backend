package kr.omong.dulpick.domain.member.application.query.view;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record MemberSocialAccount(
        SocialProvider provider,
        String email
) {
}
