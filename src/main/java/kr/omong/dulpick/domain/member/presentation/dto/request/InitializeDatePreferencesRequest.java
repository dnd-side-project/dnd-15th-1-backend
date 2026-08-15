package kr.omong.dulpick.domain.member.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;

@Schema(description = "최초 온보딩에서 선택적으로 전달하는 데이트 성향")
public record InitializeDatePreferencesRequest(
        @Schema(
                description = "실내·실외 선호. 네 필드를 설정할 때 INDOOR 또는 OUTDOOR를 입력합니다.",
                allowableValues = {"INDOOR", "OUTDOOR"},
                example = "INDOOR",
                nullable = true
        )
        String indoorOutdoor,
        @Schema(
                description = "활동 강도 선호. 네 필드를 설정할 때 ACTIVE 또는 STATIC을 입력합니다.",
                allowableValues = {"ACTIVE", "STATIC"},
                example = "ACTIVE",
                nullable = true
        )
        String activityLevel,
        @Schema(
                description = "데이트 시간대 선호. 네 필드를 설정할 때 DAY 또는 NIGHT를 입력합니다.",
                allowableValues = {"DAY", "NIGHT"},
                example = "NIGHT",
                nullable = true
        )
        String dateTime,
        @Schema(
                description = "데이트 중심 요소. 네 필드를 설정할 때 FOOD 또는 SIGHTSEEING을 입력합니다.",
                allowableValues = {"FOOD", "SIGHTSEEING"},
                example = "FOOD",
                nullable = true
        )
        String dateFocus
) {

    public DatePreferences toDomainOrNull() {
        if (allUnset()) {
            return null;
        }
        return new DatePreferences(
                parse(indoorOutdoor, "indoorOutdoor"),
                parse(activityLevel, "activityLevel"),
                parse(dateTime, "dateTime"),
                parse(dateFocus, "dateFocus")
        );
    }

    private boolean allUnset() {
        return isBlank(indoorOutdoor)
                && isBlank(activityLevel)
                && isBlank(dateTime)
                && isBlank(dateFocus);
    }

    private DatePreferenceOption parse(String rawValue, String field) {
        if (isBlank(rawValue)) {
            throw new InvalidMemberProfileException(
                    field,
                    "INVALID_DATE_PREFERENCE",
                    "데이트 성향을 설정하려면 네 항목을 모두 입력해야 합니다"
            );
        }
        try {
            return DatePreferenceOption.valueOf(rawValue.strip());
        } catch (IllegalArgumentException exception) {
            throw new InvalidMemberProfileException(
                    field,
                    "INVALID_DATE_PREFERENCE",
                    "허용되지 않는 데이트 성향입니다"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
