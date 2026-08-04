package kr.omong.dulpick.domain.testauth.application;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;

public record TestAuthResult(
        Long memberId,
        IssuedTokens tokens
) {
}
