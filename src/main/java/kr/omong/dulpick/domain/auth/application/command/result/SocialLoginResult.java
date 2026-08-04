package kr.omong.dulpick.domain.auth.application.command.result;

public record SocialLoginResult(
        Long memberId,
        boolean newMember,
        IssuedTokens tokens
) {
}
