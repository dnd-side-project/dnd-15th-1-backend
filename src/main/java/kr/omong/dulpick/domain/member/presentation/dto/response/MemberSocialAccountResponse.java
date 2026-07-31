package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.MemberSocialAccount;

public record MemberSocialAccountResponse(
        @Schema(
                description = "연결된 소셜 제공자. KAKAO는 카카오, GOOGLE은 구글, APPLE은 애플을 의미합니다.",
                allowableValues = {"KAKAO", "GOOGLE", "APPLE"},
                example = "KAKAO"
        )
        SocialProvider provider,
        String email
) {

    public static MemberSocialAccountResponse from(MemberSocialAccount account) {
        return new MemberSocialAccountResponse(account.provider(), account.email());
    }
}
