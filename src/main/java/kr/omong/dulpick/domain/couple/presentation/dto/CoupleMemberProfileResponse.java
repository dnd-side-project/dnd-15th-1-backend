package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleMemberProfile;

public record CoupleMemberProfileResponse(
        @Schema(
                description = "현재 저장된 최신 닉네임. 사용자 인식 문자 기준 1~6자입니다.",
                example = "둘픽이",
                minLength = 1,
                maxLength = 6
        )
        String nickname,
        @Schema(description = "iOS가 내장 에셋에 매핑할 프로필 아이콘 번호", example = "1", minimum = "1", maximum = "5")
        int profileIcon
) {

    public static CoupleMemberProfileResponse from(CoupleMemberProfile profile) {
        if (profile == null) {
            return null;
        }
        return new CoupleMemberProfileResponse(profile.nickname(), profile.profileIcon());
    }
}
