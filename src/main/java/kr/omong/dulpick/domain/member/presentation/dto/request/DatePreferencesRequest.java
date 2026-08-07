package kr.omong.dulpick.domain.member.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.member.domain.ActivityLevel;
import kr.omong.dulpick.domain.member.domain.DateFocus;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.DateTimePreference;
import kr.omong.dulpick.domain.member.domain.IndoorOutdoor;

public record DatePreferencesRequest(
        @NotNull IndoorOutdoor indoorOutdoor,
        @NotNull ActivityLevel activityLevel,
        @NotNull DateTimePreference dateTime,
        @NotNull DateFocus dateFocus
) {

    public DatePreferences toDomain() {
        return new DatePreferences(indoorOutdoor, activityLevel, dateTime, dateFocus);
    }
}
