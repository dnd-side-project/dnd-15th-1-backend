package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;

public record DatePreferences(
        DatePreferenceOption indoorOutdoor,
        DatePreferenceOption activityLevel,
        DatePreferenceOption dateTime,
        DatePreferenceOption dateFocus
) {

    public DatePreferences {
        validateCategory(
                indoorOutdoor,
                DatePreferenceOption.Category.INDOOR_OUTDOOR,
                "indoorOutdoor"
        );
        validateCategory(
                activityLevel,
                DatePreferenceOption.Category.ACTIVITY_LEVEL,
                "activityLevel"
        );
        validateCategory(dateTime, DatePreferenceOption.Category.DATE_TIME, "dateTime");
        validateCategory(dateFocus, DatePreferenceOption.Category.DATE_FOCUS, "dateFocus");
    }

    private static void validateCategory(
            DatePreferenceOption option,
            DatePreferenceOption.Category category,
            String field
    ) {
        if (option == null || !option.belongsTo(category)) {
            throw new InvalidMemberProfileException(
                    field,
                    "INVALID_DATE_PREFERENCE",
                    "해당 항목에서 허용하는 데이트 성향이 아닙니다"
            );
        }
    }
}
