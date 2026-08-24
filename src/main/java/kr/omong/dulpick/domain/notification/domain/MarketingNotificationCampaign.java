package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "marketing_notification_campaigns")
public class MarketingNotificationCampaign {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketingNotificationCampaignStatus status;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "queued_count", nullable = false)
    private int queuedCount;

    @Column(name = "last_member_id", nullable = false)
    private long lastMemberId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected MarketingNotificationCampaign() {
    }

    private MarketingNotificationCampaign(
            String id,
            String title,
            String body,
            int targetCount,
            Instant createdAt
    ) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.status = MarketingNotificationCampaignStatus.PENDING;
        this.targetCount = targetCount;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static MarketingNotificationCampaign create(
            String id,
            String title,
            String body,
            int targetCount,
            Instant createdAt
    ) {
        if (id == null || id.isBlank() || title == null || title.isBlank()
                || body == null || body.isBlank() || targetCount < 0 || createdAt == null) {
            throw new IllegalArgumentException("Marketing notification campaign is invalid");
        }
        return new MarketingNotificationCampaign(id, title, body, targetCount, createdAt);
    }

    public void claim(Instant claimedAt) {
        status = MarketingNotificationCampaignStatus.PROCESSING;
        updatedAt = claimedAt;
    }

    public void advance(
            long lastMemberId,
            int queuedCount,
            boolean hasMore,
            Instant advancedAt
    ) {
        this.lastMemberId = Math.max(this.lastMemberId, lastMemberId);
        this.queuedCount += Math.max(queuedCount, 0);
        this.status = hasMore
                ? MarketingNotificationCampaignStatus.PENDING
                : MarketingNotificationCampaignStatus.COMPLETED;
        this.updatedAt = advancedAt;
        this.completedAt = hasMore ? null : advancedAt;
    }

    public Long getLastMemberId() {
        return lastMemberId;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public MarketingNotificationCampaignStatus getStatus() {
        return status;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getQueuedCount() {
        return queuedCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
