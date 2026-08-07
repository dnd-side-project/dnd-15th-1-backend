package kr.omong.dulpick.domain.member.presentation.dto.response;

import kr.omong.dulpick.domain.member.application.query.view.MemberDatePreferences;
import kr.omong.dulpick.domain.member.domain.ActivityLevel;
import kr.omong.dulpick.domain.member.domain.DateFocus;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.DateTimePreference;
import kr.omong.dulpick.domain.member.domain.IndoorOutdoor;

public record MemberDatePreferencesResponse(
        IndoorOutdoor indoorOutdoor,
        ActivityLevel activityLevel,
        DateTimePreference dateTime,
        DateFocus dateFocus
) {

    public static MemberDatePreferencesResponse from(MemberDatePreferences preferences) {
        if (preferences == null) {
            return null;
        }
        return new MemberDatePreferencesResponse(
                preferences.indoorOutdoor(),
                preferences.activityLevel(),
                preferences.dateTime(),
                preferences.dateFocus()
        );
    }

    public static MemberDatePreferencesResponse from(DatePreferences preferences) {
        return new MemberDatePreferencesResponse(
                preferences.indoorOutdoor(),
                preferences.activityLevel(),
                preferences.dateTime(),
                preferences.dateFocus()
        );
    }
}
