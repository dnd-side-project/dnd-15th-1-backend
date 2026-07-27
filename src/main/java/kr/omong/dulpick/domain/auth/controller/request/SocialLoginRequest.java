package kr.omong.dulpick.domain.auth.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.application.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record SocialLoginRequest(
        @NotNull SocialProvider provider,
        @NotBlank String idToken,
        String authorizationCode,
        String nonce
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, idToken, authorizationCode, nonce);
    }
}
