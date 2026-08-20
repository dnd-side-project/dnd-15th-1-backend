package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.domain.DatePreferences;

public final class PlaceDateTraitMatcher {

    private PlaceDateTraitMatcher() {
    }

    public static int score(DatePreferences preferences, PlaceDateTraitsView traits) {
        if (preferences == null || traits == null) {
            return 0;
        }
        int score = 0;
        if (sameName(preferences.indoorOutdoor(), traits.environment())) {
            score++;
        }
        if (sameName(preferences.activityLevel(), traits.activity())) {
            score++;
        }
        if (sameName(preferences.dateTime(), traits.time())) {
            score++;
        }
        if (sameName(preferences.dateFocus(), traits.focus())) {
            score++;
        }
        return score;
    }

    private static boolean sameName(Enum<?> left, Enum<?> right) {
        return left != null && right != null && left.name().equals(right.name());
    }
}
