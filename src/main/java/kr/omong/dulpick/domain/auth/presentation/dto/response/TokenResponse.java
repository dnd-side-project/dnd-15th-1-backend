package kr.omong.dulpick.domain.auth.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;

public record TokenResponse(
        @Schema(description = "Authorization 헤더에 사용할 토큰 타입", example = "Bearer")
        String tokenType,
        @Schema(description = "둘픽 API Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Access Token 재발급 및 로그아웃에 사용할 Refresh Token")
        String refreshToken,
        @Schema(description = "Access Token 남은 유효 시간(초)", example = "900")
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
