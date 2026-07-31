package kr.omong.dulpick.domain.auth.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.application.SocialLoginResult;

public record SocialLoginResponse(
        @Schema(description = "둘픽 회원 식별자", example = "1")
        Long memberId,
        @Schema(description = "이번 요청에서 신규 회원이 생성되었는지 여부", example = "true")
        boolean newMember,
        @Schema(description = "둘픽 API 인증에 사용할 자체 토큰")
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
