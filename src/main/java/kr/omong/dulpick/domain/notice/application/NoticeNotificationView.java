package kr.omong.dulpick.domain.notice.application;

import kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaign;

import java.time.Instant;

public record NoticeNotificationView(
        String campaignId,
        String status,
        int targetCount,
        int queuedCount,
        Instant queuedAt
) {

    public static NoticeNotificationView from(NoticeNotificationCampaign campaign) {
        return new NoticeNotificationView(
                campaign.getId(),
                campaign.getStatus().name(),
                campaign.getTargetCount(),
                campaign.getQueuedCount(),
                campaign.getCreatedAt()
        );
    }
}
