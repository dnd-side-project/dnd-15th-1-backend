package kr.omong.dulpick.domain.auth.application;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
