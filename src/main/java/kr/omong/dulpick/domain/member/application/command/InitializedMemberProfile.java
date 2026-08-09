package kr.omong.dulpick.domain.member.application.command;

import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;
import kr.omong.dulpick.domain.member.domain.DatePreferences;

public record InitializedMemberProfile(
        String nickname,
        int profileIcon,
        DatePreferences datePreferences,
        IssuedConnectionCode connectionCode
) {
}
