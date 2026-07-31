package kr.omong.dulpick.domain.testauth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.presentation.dto.response.TokenResponse;
import kr.omong.dulpick.domain.testauth.application.TestAuthResult;

public record TestAuthResponse(
        @Schema(description = "생성되거나 로그인한 둘픽 회원 식별자", example = "1")
        Long memberId,
        @Schema(
                description = "인증2 계정에 연결되는 소셜 제공자. 항상 KAKAO입니다.",
                allowableValues = "KAKAO",
                example = "KAKAO"
        )
        SocialProvider provider,
        @Schema(description = "다른 둘픽 API 호출에 사용할 자체 Access Token과 Refresh Token")
        TokenResponse token
) {

    public static TestAuthResponse from(TestAuthResult result) {
        return new TestAuthResponse(
                result.memberId(),
                SocialProvider.KAKAO,
                TokenResponse.from(result.tokens())
        );
    }
}
