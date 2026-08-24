package kr.omong.dulpick.domain.notification.application.admin;

import java.time.Instant;

public record MarketingNotificationSendView(
        String campaignId,
        int targetCount,
        Instant queuedAt
) {
}
