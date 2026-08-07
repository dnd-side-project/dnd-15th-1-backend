package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;

public record MemberDatePreferencesResponse(
        @Schema(
                description = "실내·실외 선호",
                allowableValues = {"INDOOR", "OUTDOOR"},
                example = "INDOOR"
        )
        DatePreferenceOption indoorOutdoor,
        @Schema(
                description = "활동 강도 선호",
                allowableValues = {"ACTIVE", "STATIC"},
                example = "ACTIVE"
        )
        DatePreferenceOption activityLevel,
        @Schema(
                description = "데이트 시간대 선호",
                allowableValues = {"DAY", "NIGHT"},
                example = "NIGHT"
        )
        DatePreferenceOption dateTime,
        @Schema(
                description = "데이트 중심 요소",
                allowableValues = {"FOOD", "SIGHTSEEING"},
                example = "FOOD"
        )
        DatePreferenceOption dateFocus
) {

    public static MemberDatePreferencesResponse from(DatePreferences preferences) {
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

}
