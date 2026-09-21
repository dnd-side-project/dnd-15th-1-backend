package kr.omong.dulpick.domain.analytics.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.analytics.application.AnalyticsFunnelView;

import java.util.List;

public record AnalyticsFunnelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Step> steps
) {

    public static AnalyticsFunnelResponse from(AnalyticsFunnelView view) {
        return new AnalyticsFunnelResponse(view.steps().stream().map(Step::from).toList());
    }

    public record Step(
            @Schema(example = "가입")
            String name,
            @Schema(example = "20")
            long count,
            @Schema(example = "0.6", nullable = true)
            Double conversionFromPrevious,
            @Schema(example = "0.4", nullable = true)
            Double dropoutFromPrevious
    ) {

        private static Step from(AnalyticsFunnelView.Step step) {
            return new Step(
                    step.name(),
                    step.count(),
                    step.conversionFromPrevious(),
                    step.dropoutFromPrevious()
            );
        }
    }
}
