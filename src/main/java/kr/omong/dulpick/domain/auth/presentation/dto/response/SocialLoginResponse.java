package kr.omong.dulpick.domain.auth.presentation.dto.response;

import kr.omong.dulpick.domain.auth.application.SocialLoginResult;

public record SocialLoginResponse(
        Long memberId,
        boolean newMember,
        TokenResponse token
) {

    public static SocialLoginResponse from(SocialLoginResult result) {
        return new SocialLoginResponse(
                result.memberId(),
                result.newMember(),
                TokenResponse.from(result.tokens())
        );
    }
}
