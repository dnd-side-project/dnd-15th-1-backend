package kr.omong.dulpick.domain.analytics.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.analytics.application.AnalyticsRetentionView;

import java.util.List;

public record AnalyticsRetentionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Period> periods
) {

    public static AnalyticsRetentionResponse from(AnalyticsRetentionView view) {
        return new AnalyticsRetentionResponse(view.periods().stream().map(Period::from).toList());
    }

    public record Period(
            @Schema(example = "W1")
            String name,
            @Schema(example = "20")
            long cohortSize,
            @Schema(example = "8")
            long retainedMembers,
            @Schema(example = "0.4", nullable = true)
            Double rate
    ) {

        private static Period from(AnalyticsRetentionView.Period period) {
            return new Period(
                    period.name(),
                    period.cohortSize(),
                    period.retainedMembers(),
                    period.rate()
            );
        }
    }
}
