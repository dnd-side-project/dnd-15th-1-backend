package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceDateTraitMatcherTest {

    @Test
    void countsMatchingDateTraits() {
        DatePreferences preferences = new DatePreferences(
                DatePreferenceOption.INDOOR,
                DatePreferenceOption.ACTIVE,
                DatePreferenceOption.DAY,
                DatePreferenceOption.FOOD
        );
        PlaceDateTraitsView traits = new PlaceDateTraitsView(
                PlaceClassificationStatus.CLASSIFIED,
                PlaceEnvironment.INDOOR,
                PlaceActivity.STATIC,
                PlaceTime.DAY,
                PlaceFocus.FOOD
        );

        assertThat(PlaceDateTraitMatcher.score(preferences, traits)).isEqualTo(3);
        assertThat(PlaceDateTraitMatcher.score(null, traits)).isZero();
    }
}
