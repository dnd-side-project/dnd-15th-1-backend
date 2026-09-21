package kr.omong.dulpick.domain.analytics.application;

import java.util.List;

public record AnalyticsRetentionView(List<Period> periods) {

    public record Period(
            String name,
            long cohortSize,
            long retainedMembers,
            Double rate
    ) {
    }
}
