package kr.omong.dulpick.domain.notice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notice_notification_campaigns")
public class NoticeNotificationCampaign {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeNotificationCampaignStatus status;

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

    protected NoticeNotificationCampaign() {
    }

    private NoticeNotificationCampaign(
            String id,
            Long noticeId,
            String title,
            String body,
            int targetCount,
            Instant createdAt
    ) {
        this.id = id;
        this.noticeId = noticeId;
        this.title = title;
        this.body = body;
        this.status = NoticeNotificationCampaignStatus.PENDING;
        this.targetCount = targetCount;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static NoticeNotificationCampaign create(
            String id,
            Long noticeId,
            String title,
            String body,
            int targetCount,
            Instant createdAt
    ) {
        if (id == null || id.isBlank() || noticeId == null || title == null || title.isBlank()
                || body == null || body.isBlank() || targetCount < 0 || createdAt == null) {
            throw new IllegalArgumentException("Notice notification campaign is invalid");
        }
        return new NoticeNotificationCampaign(id, noticeId, title, body, targetCount, createdAt);
    }

    public void claim(Instant claimedAt) {
        status = NoticeNotificationCampaignStatus.PROCESSING;
        updatedAt = claimedAt;
    }

    public void advance(long lastMemberId, int queuedCount, boolean hasMore, Instant advancedAt) {
        this.lastMemberId = Math.max(this.lastMemberId, lastMemberId);
        this.queuedCount += Math.max(queuedCount, 0);
        this.status = hasMore
                ? NoticeNotificationCampaignStatus.PENDING
                : NoticeNotificationCampaignStatus.COMPLETED;
        this.updatedAt = advancedAt;
        this.completedAt = hasMore ? null : advancedAt;
    }

    public String getId() {
        return id;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NoticeNotificationCampaignStatus getStatus() {
        return status;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getQueuedCount() {
        return queuedCount;
    }

    public long getLastMemberId() {
        return lastMemberId;
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
