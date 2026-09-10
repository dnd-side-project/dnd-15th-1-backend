package kr.omong.dulpick.domain.analytics.application;

import java.util.List;

public record AnalyticsFunnelView(List<Step> steps) {

    public record Step(
            String name,
            long count,
            Double conversionFromPrevious,
            Double dropoutFromPrevious
    ) {
    }
}
