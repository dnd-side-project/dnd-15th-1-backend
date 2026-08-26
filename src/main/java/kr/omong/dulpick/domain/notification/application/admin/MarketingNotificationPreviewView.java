package kr.omong.dulpick.domain.notification.application.admin;

import java.time.Instant;

public record MarketingNotificationPreviewView(
        int targetCount,
        Instant calculatedAt
) {
}
