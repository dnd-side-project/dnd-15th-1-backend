package kr.omong.dulpick.domain.member.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;

public record DatePreferencesRequest(
        @NotNull
        @Schema(
                description = "실내·실외 선호: INDOOR(실내), OUTDOOR(실외)",
                allowableValues = {"INDOOR", "OUTDOOR"},
                example = "INDOOR"
        )
        DatePreferenceOption indoorOutdoor,
        @NotNull
        @Schema(
                description = "활동 강도 선호: ACTIVE(액티비티), STATIC(정적 활동)",
                allowableValues = {"ACTIVE", "STATIC"},
                example = "ACTIVE"
        )
        DatePreferenceOption activityLevel,
        @NotNull
        @Schema(
                description = "데이트 시간대 선호: DAY(낮), NIGHT(밤)",
                allowableValues = {"DAY", "NIGHT"},
                example = "NIGHT"
        )
        DatePreferenceOption dateTime,
        @NotNull
        @Schema(
                description = "데이트 중심 요소: FOOD(식사), SIGHTSEEING(볼거리)",
                allowableValues = {"FOOD", "SIGHTSEEING"},
                example = "FOOD"
        )
        DatePreferenceOption dateFocus
) {

    public DatePreferences toDomain() {
        return new DatePreferences(indoorOutdoor, activityLevel, dateTime, dateFocus);
    }
}
