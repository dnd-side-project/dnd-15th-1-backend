package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;

public record DatePreferences(
        DatePreferenceOption indoorOutdoor,
        DatePreferenceOption activityLevel,
        DatePreferenceOption dateTime,
        DatePreferenceOption dateFocus
) {

    public DatePreferences {
        validateCategory(indoorOutdoor, DatePreferenceOption.Category.INDOOR_OUTDOOR);
        validateCategory(activityLevel, DatePreferenceOption.Category.ACTIVITY_LEVEL);
        validateCategory(dateTime, DatePreferenceOption.Category.DATE_TIME);
        validateCategory(dateFocus, DatePreferenceOption.Category.DATE_FOCUS);
    }

    private static void validateCategory(
            DatePreferenceOption option,
            DatePreferenceOption.Category category
    ) {
        if (option == null || !option.belongsTo(category)) {
            throw new InvalidMemberProfileException();
        }
    }
}
