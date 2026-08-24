package kr.omong.dulpick.domain.notification.application.admin;

import java.time.Instant;

import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaign;

public record MarketingNotificationSendView(
        String campaignId,
        String status,
        int targetCount,
        int queuedCount,
        Instant queuedAt
) {

    public static MarketingNotificationSendView from(MarketingNotificationCampaign campaign) {
        return new MarketingNotificationSendView(
                campaign.getId(),
                campaign.getStatus().name(),
                campaign.getTargetCount(),
                campaign.getQueuedCount(),
                campaign.getCreatedAt()
        );
    }
}
