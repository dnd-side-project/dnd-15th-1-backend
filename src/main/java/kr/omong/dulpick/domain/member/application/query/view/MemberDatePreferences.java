package kr.omong.dulpick.domain.member.application.query.view;

import kr.omong.dulpick.domain.member.domain.ActivityLevel;
import kr.omong.dulpick.domain.member.domain.DateFocus;
import kr.omong.dulpick.domain.member.domain.DateTimePreference;
import kr.omong.dulpick.domain.member.domain.IndoorOutdoor;

public record MemberDatePreferences(
        IndoorOutdoor indoorOutdoor,
        ActivityLevel activityLevel,
        DateTimePreference dateTime,
        DateFocus dateFocus
) {
}
