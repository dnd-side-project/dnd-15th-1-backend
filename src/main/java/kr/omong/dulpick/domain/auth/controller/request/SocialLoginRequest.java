package kr.omong.dulpick.domain.auth.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.application.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record SocialLoginRequest(
        @NotNull SocialProvider provider,
        @NotBlank String idToken,
        String authorizationCode,
        @Schema(
                description = "nonce 발급 API에서 받은 원문이며 백엔드에는 항상 원문을 전달합니다. "
                        + "제공자 인증 요청에는 Google/Kakao는 원문, Apple은 SHA-256 해시를 사용합니다."
        )
        @NotBlank String nonce
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, idToken, authorizationCode, nonce);
    }
}
