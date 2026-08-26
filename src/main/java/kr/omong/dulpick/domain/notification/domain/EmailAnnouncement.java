package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_announcements")
public class EmailAnnouncement {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED_LOG_ONLY = "COMPLETED_LOG_ONLY";
    public static final String STATUS_COMPLETED_SMTP = "COMPLETED_SMTP";
    public static final String DELIVERY_LOG_ONLY = "LOG_ONLY";

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private String status;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "delivery_mode", nullable = false)
    private String deliveryMode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmailAnnouncement() {
    }

    public static EmailAnnouncement create(
            String category,
            String title,
            String body,
            int targetCount,
            Instant now
    ) {
        EmailAnnouncement announcement = new EmailAnnouncement();
        announcement.id = UUID.randomUUID().toString();
        announcement.category = category;
        announcement.title = title;
        announcement.body = body;
        announcement.status = STATUS_PENDING;
        announcement.targetCount = targetCount;
        announcement.deliveryMode = DELIVERY_LOG_ONLY;
        announcement.createdAt = now;
        announcement.updatedAt = now;
        return announcement;
    }

    public void complete(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
