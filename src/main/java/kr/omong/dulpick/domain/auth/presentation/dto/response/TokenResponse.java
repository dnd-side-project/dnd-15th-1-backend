package kr.omong.dulpick.domain.auth.presentation.dto.response;

import kr.omong.dulpick.domain.auth.application.IssuedTokens;

public record TokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
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
