package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.domain.ClassificationSource;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceClassificationTest {

    @Test
    void representsPartialClassificationPerAxis() {
        PlaceClassification classification = PlaceClassification.initialize(
                10L,
                Instant.parse("2026-08-18T00:00:00Z")
        );

        classification.classifyActivity(
                PlaceActivity.ACTIVE,
                ClassificationSource.AI,
                Instant.parse("2026-08-18T00:01:00Z")
        );
        classification.classifyFocus(
                PlaceFocus.SIGHTSEEING,
                ClassificationSource.MANUAL,
                Instant.parse("2026-08-18T00:02:00Z")
        );

        assertThat(classification.getStatus())
                .isEqualTo(PlaceClassificationStatus.PARTIALLY_CLASSIFIED);
        assertThat(classification.getEnvironment()).isNull();
        assertThat(classification.getActivity()).isEqualTo(PlaceActivity.ACTIVE);
        assertThat(classification.getActivitySource()).isEqualTo(ClassificationSource.AI);
        assertThat(classification.getFocusSource()).isEqualTo(ClassificationSource.MANUAL);
    }

    @Test
    void doesNotOverwriteManualValueWithAi() {
        PlaceClassification classification = PlaceClassification.initialize(
                10L,
                Instant.parse("2026-08-18T00:00:00Z")
        );
        classification.classifyEnvironment(
                PlaceEnvironment.INDOOR,
                ClassificationSource.MANUAL,
                Instant.parse("2026-08-18T00:01:00Z")
        );

        classification.classifyEnvironment(
                PlaceEnvironment.OUTDOOR,
                ClassificationSource.AI,
                Instant.parse("2026-08-18T00:02:00Z")
        );

        assertThat(classification.getEnvironment()).isEqualTo(PlaceEnvironment.INDOOR);
        assertThat(classification.getEnvironmentSource()).isEqualTo(ClassificationSource.MANUAL);
    }

    @Test
    void reportsClassifiedOnlyWhenAllAxesHaveValues() {
        PlaceClassification classification = PlaceClassification.initialize(
                10L,
                Instant.parse("2026-08-18T00:00:00Z")
        );
        Instant now = Instant.parse("2026-08-18T00:01:00Z");

        classification.classifyEnvironment(PlaceEnvironment.INDOOR, ClassificationSource.AI, now);
        classification.classifyActivity(PlaceActivity.STATIC, ClassificationSource.AI, now);
        classification.classifyTime(PlaceTime.NIGHT, ClassificationSource.AI, now);
        classification.classifyFocus(PlaceFocus.FOOD, ClassificationSource.AI, now);

        assertThat(classification.getStatus()).isEqualTo(PlaceClassificationStatus.CLASSIFIED);

        classification.clearTime(Instant.parse("2026-08-18T00:02:00Z"));

        assertThat(classification.getStatus())
                .isEqualTo(PlaceClassificationStatus.PARTIALLY_CLASSIFIED);
        assertThat(classification.getTime()).isNull();
        assertThat(classification.getTimeSource()).isNull();
    }
}
