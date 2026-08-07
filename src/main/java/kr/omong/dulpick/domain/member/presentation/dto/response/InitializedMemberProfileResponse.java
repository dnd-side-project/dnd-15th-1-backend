package kr.omong.dulpick.domain.member.presentation.dto.response;

import kr.omong.dulpick.domain.member.application.command.InitializedMemberProfile;

public record InitializedMemberProfileResponse(
        String nickname,
        int profileIcon,
        MemberDatePreferencesResponse datePreferences,
        String connectionCode,
        String shareUrl
) {

    public static InitializedMemberProfileResponse from(InitializedMemberProfile profile) {
        return new InitializedMemberProfileResponse(
                profile.nickname(),
                profile.profileIcon(),
                MemberDatePreferencesResponse.from(profile.datePreferences()),
                profile.connectionCode().code(),
                profile.connectionCode().shareUrl()
        );
    }
}
