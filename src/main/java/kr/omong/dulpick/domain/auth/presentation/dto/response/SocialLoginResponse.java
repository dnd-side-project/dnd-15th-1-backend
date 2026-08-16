package kr.omong.dulpick.domain.auth.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;

public record SocialLoginResponse(
        @Schema(description = "둘픽 회원 식별자", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long memberId,
        @Schema(
                description = "이번 로그인에서 회원 계정이 새로 생성되었는지 여부. 온보딩 완료 여부와는 별개입니다.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean newMember,
        @Schema(
                description = "프로필과 데이트 성향을 포함한 최초 온보딩 완료 여부. 로그인 후 화면 분기에 사용합니다.",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean onboardingCompleted,
        @Schema(
                description = "둘픽 API 인증에 사용할 Access·Refresh Token 묶음입니다.",
                example = "{\"tokenType\":\"Bearer\",\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9.example.access\",\"refreshToken\":\"eyJhbGciOiJIUzI1NiJ9.example.refresh\",\"expiresIn\":900}",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        TokenResponse token
) {

    public static SocialLoginResponse from(SocialLoginResult result) {
        return new SocialLoginResponse(
                result.memberId(),
                result.newMember(),
                result.onboardingCompleted(),
                TokenResponse.from(result.tokens())
        );
    }
}
