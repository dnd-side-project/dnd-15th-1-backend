package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleMemberProfile;

@Schema(description = "커플 구성원의 공개 기본 프로필")
public record CoupleMemberProfileResponse(
        @Schema(
                description = "현재 저장된 최신 닉네임. 사용자 인식 문자 기준 1~6자입니다.",
                example = "둘픽이",
                minLength = 1,
                maxLength = 6
        )
        String nickname,
        @Schema(description = "iOS 프로필 에셋 번호(1~5)", example = "1", minimum = "1", maximum = "5")
        int profileIcon
) {

    public static CoupleMemberProfileResponse from(CoupleMemberProfile profile) {
        if (profile == null) {
            return null;
        }
        return new CoupleMemberProfileResponse(profile.nickname(), profile.profileIcon());
    }
}
