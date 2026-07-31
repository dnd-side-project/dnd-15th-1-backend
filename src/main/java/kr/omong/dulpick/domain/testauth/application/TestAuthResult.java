package kr.omong.dulpick.domain.testauth.application;

import kr.omong.dulpick.domain.auth.application.IssuedTokens;

public record TestAuthResult(
        Long memberId,
        IssuedTokens tokens
) {
}
