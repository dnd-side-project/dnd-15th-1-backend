package kr.omong.dulpick.domain.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.application.command.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record SocialLoginRequest(
        @Schema(
                description = "ID Token을 발급한 소셜 제공자",
                allowableValues = {"KAKAO", "GOOGLE", "APPLE"},
                example = "KAKAO"
        )
        @NotNull SocialProvider provider,
        @Schema(
                description = "provider SDK가 발급한 OIDC ID Token 원문",
                example = "eyJraWQiOiJ..."
        )
        @NotBlank String idToken,
        @Schema(
                description = "Apple 로그인에서 사용하는 일회성 authorization code",
                nullable = true,
                example = "c8f7a1..."
        )
        String authorizationCode,
        @Schema(
                description = "nonce 발급 API에서 받은 원문",
                example = "l7JcLxgJx7c0nS0wqgWQeQ"
        )
        @NotBlank String nonce
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, idToken, authorizationCode, nonce);
    }
}
