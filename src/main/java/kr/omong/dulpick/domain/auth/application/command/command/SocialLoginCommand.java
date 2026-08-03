package kr.omong.dulpick.domain.auth.application.command.command;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record SocialLoginCommand(
        SocialProvider provider,
        String idToken,
        String authorizationCode,
        String nonce
) {
}
