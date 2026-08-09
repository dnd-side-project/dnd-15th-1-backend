package kr.omong.dulpick.domain.member.domain;

public enum DatePreferenceOption {

    INDOOR(Category.INDOOR_OUTDOOR),
    OUTDOOR(Category.INDOOR_OUTDOOR),
    ACTIVE(Category.ACTIVITY_LEVEL),
    STATIC(Category.ACTIVITY_LEVEL),
    DAY(Category.DATE_TIME),
    NIGHT(Category.DATE_TIME),
    FOOD(Category.DATE_FOCUS),
    SIGHTSEEING(Category.DATE_FOCUS);

    private final Category category;

    DatePreferenceOption(Category category) {
        this.category = category;
    }

    public boolean belongsTo(Category category) {
        return this.category == category;
    }

    public enum Category {
        INDOOR_OUTDOOR,
        ACTIVITY_LEVEL,
        DATE_TIME,
        DATE_FOCUS
    }
}
