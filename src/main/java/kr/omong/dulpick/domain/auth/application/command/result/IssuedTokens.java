package kr.omong.dulpick.domain.auth.application.command.result;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn
) {
}
