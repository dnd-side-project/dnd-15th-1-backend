package kr.omong.dulpick.domain.member.presentation.dto.response;

import kr.omong.dulpick.domain.member.application.command.UpdatedMemberProfile;

public record UpdatedMemberProfileResponse(
        String nickname,
        int profileIcon
) {

    public static UpdatedMemberProfileResponse from(UpdatedMemberProfile profile) {
        return new UpdatedMemberProfileResponse(profile.nickname(), profile.profileIcon());
    }
}
