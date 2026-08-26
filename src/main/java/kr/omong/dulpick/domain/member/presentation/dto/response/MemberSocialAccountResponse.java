package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.query.view.MemberSocialAccount;

public record MemberSocialAccountResponse(
        @Schema(
                description = "연결된 소셜 제공자",
                allowableValues = {"KAKAO", "GOOGLE", "APPLE"},
                example = "KAKAO",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        SocialProvider provider,
        @Schema(description = "소셜 로그인 계정 이메일. 제공자가 이메일을 주지 않으면 null입니다.", example = "member@example.com", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String email
) {

    public static MemberSocialAccountResponse from(MemberSocialAccount account) {
        return new MemberSocialAccountResponse(account.provider(), account.email());
    }
}
