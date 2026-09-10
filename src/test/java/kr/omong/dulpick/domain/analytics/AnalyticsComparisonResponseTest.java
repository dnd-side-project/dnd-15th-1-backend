package kr.omong.dulpick.domain.analytics;

import kr.omong.dulpick.domain.analytics.application.AnalyticsMetricsView;
import kr.omong.dulpick.domain.analytics.presentation.AnalyticsComparisonResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsComparisonResponseTest {

    @Test
    void keepsComparisonAvailableWhenPreviousPeriodHasNoRatioData() {
        AnalyticsComparisonResponse response = AnalyticsComparisonResponse.from(
                metrics(0.5),
                metrics(null)
        );

        assertThat(response.metrics()).hasSize(21);
        assertThat(response.metrics().get(8).currentValue()).isEqualTo(0.5);
        assertThat(response.metrics().get(8).previousValue()).isNull();
        assertThat(response.metrics().get(8).difference()).isNull();
        assertThat(response.metrics().get(8).changeRate()).isNull();
    }

    private AnalyticsMetricsView metrics(Double ratio) {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-10T00:00:00Z");
        return new AnalyticsMetricsView(
                from, to,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                ratio, ratio, ratio, ratio, ratio, ratio,
                ratio, ratio, ratio, ratio, ratio, ratio, ratio
        );
    }
}
