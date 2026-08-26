package kr.omong.dulpick.domain.place.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceAnalysisPropertiesTest {

    @Test
    void allowsConfiguredConcurrencyWithinRaisedLimits() {
        PlaceAnalysisProperties properties = new PlaceAnalysisProperties(
                true, 100, 10, 1, true, 600, 300, 3,
                Duration.ofSeconds(5), 20, 8, 12
        );

        assertThat(properties.workerConcurrency()).isEqualTo(8);
        assertThat(properties.verificationConcurrency()).isEqualTo(12);
    }

    @Test
    void clampsConcurrencyAboveSupportedLimits() {
        PlaceAnalysisProperties properties = new PlaceAnalysisProperties(
                true, 100, 10, 1, true, 600, 300, 3,
                Duration.ofSeconds(5), 20, 99, 99
        );

        assertThat(properties.workerConcurrency()).isEqualTo(16);
        assertThat(properties.verificationConcurrency()).isEqualTo(20);
    }
}
