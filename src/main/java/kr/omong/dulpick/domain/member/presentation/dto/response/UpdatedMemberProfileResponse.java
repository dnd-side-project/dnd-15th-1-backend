package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.member.application.command.UpdatedMemberProfile;

public record UpdatedMemberProfileResponse(
        @Schema(description = "앞뒤 공백이 제거되어 저장된 최신 닉네임", example = "둘픽이")
        String nickname,
        @Schema(
                description = "iOS 프로필 에셋 번호(1~5)",
                minimum = "1",
                maximum = "5",
                example = "3"
        )
        int profileIcon
) {

    public static UpdatedMemberProfileResponse from(UpdatedMemberProfile profile) {
        return new UpdatedMemberProfileResponse(profile.nickname(), profile.profileIcon());
    }
}
