package kr.omong.dulpick.domain.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.application.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record SocialLoginRequest(
        @Schema(
                description = "ID Token을 발급한 소셜 제공자. "
                        + "KAKAO는 카카오, GOOGLE은 구글, APPLE은 애플을 의미합니다.",
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
                description = "Apple SDK가 발급한 일회성 authorization code. "
                        + "Apple 로그인에서만 사용하며 전달을 권장합니다.",
                nullable = true,
                example = "c8f7a1..."
        )
        String authorizationCode,
        @Schema(
                description = "nonce 발급 API에서 받은 원문이며 백엔드에는 항상 원문을 전달합니다. "
                        + "제공자 인증 요청에는 Google/Kakao는 원문, Apple은 SHA-256 해시를 사용합니다.",
                example = "l7JcLxgJx7c0nS0wqgWQeQ"
        )
        @NotBlank String nonce
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, idToken, authorizationCode, nonce);
    }
}
