package kr.omong.dulpick.domain.auth.application;

public record SocialLoginResult(
        Long memberId,
        boolean newMember,
        IssuedTokens tokens
) {
}
