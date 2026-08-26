package kr.omong.dulpick.domain.auth.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;

public record TokenResponse(
        @Schema(description = "Authorization 헤더에 사용할 토큰 타입", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
        String tokenType,
        @Schema(description = "둘픽 API 요청의 Authorization 헤더에 사용하는 Access Token", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,
        @Schema(description = "Access Token 재발급 및 로그아웃에 사용하는 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9.example.refresh", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken,
        @Schema(description = "Access Token 남은 유효 시간(초)", example = "900", requiredMode = Schema.RequiredMode.REQUIRED)
        long expiresIn
) {

    public static TokenResponse from(IssuedTokens tokens) {
        return new TokenResponse(
                "Bearer",
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessTokenExpiresIn()
        );
    }
}
