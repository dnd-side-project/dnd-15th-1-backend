package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;

public record DatePreferences(
        IndoorOutdoor indoorOutdoor,
        ActivityLevel activityLevel,
        DateTimePreference dateTime,
        DateFocus dateFocus
) {

    public DatePreferences {
        if (indoorOutdoor == null
                || activityLevel == null
                || dateTime == null
                || dateFocus == null) {
            throw new InvalidMemberProfileException();
        }
    }
}
