package kr.omong.dulpick.domain.member.application.command;

import kr.omong.dulpick.domain.member.domain.DatePreferences;

public record InitializeMemberProfileCommand(
        String nickname,
        int profileIcon,
        DatePreferences datePreferences
) {
}
