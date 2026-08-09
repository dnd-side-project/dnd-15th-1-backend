package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_member_id", nullable = false)
    private Member receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationRoute route;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "deduplication_key", nullable = false, length = 200)
    private String deduplicationKey;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    private Notification(
            Member receiver,
            NotificationType type,
            String title,
            String body,
            NotificationRoute route,
            String referenceId,
            String deduplicationKey,
            Instant createdAt
    ) {
        this.receiver = receiver;
        this.type = type;
        this.title = title;
        this.body = body;
        this.route = route;
        this.referenceId = referenceId;
        this.deduplicationKey = deduplicationKey;
        this.createdAt = createdAt;
    }

    public static Notification create(
            Member receiver,
            NotificationType type,
            String title,
            String body,
            NotificationRoute route,
            String referenceId,
            String deduplicationKey,
            Instant createdAt
    ) {
        if (receiver == null || !receiver.isActive()) {
            throw new IllegalArgumentException("Active notification receiver is required");
        }
        return new Notification(
                receiver,
                type,
                title,
                body,
                route,
                referenceId,
                deduplicationKey,
                createdAt
        );
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }

    public Long getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NotificationRoute getRoute() {
        return route;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
