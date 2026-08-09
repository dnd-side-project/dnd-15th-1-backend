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

import java.time.Instant;
import java.time.Duration;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "push_device_id", nullable = false)
    private PushDevice pushDevice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PushProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationDelivery() {
    }

    private NotificationDelivery(
            Notification notification,
            PushDevice pushDevice,
            Instant createdAt
    ) {
        this.notification = notification;
        this.pushDevice = pushDevice;
        this.provider = pushDevice.getProvider();
        this.status = NotificationDeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static NotificationDelivery pending(
            Notification notification,
            PushDevice pushDevice,
            Instant createdAt
    ) {
        if (notification == null || pushDevice == null) {
            throw new IllegalArgumentException("Notification delivery target is required");
        }
        return new NotificationDelivery(notification, pushDevice, createdAt);
    }

    public Long getId() {
        return id;
    }

    public boolean canClaim(Instant now, Duration sendingTimeout) {
        if (status == NotificationDeliveryStatus.PENDING
                || status == NotificationDeliveryStatus.RETRY_PENDING) {
            return !nextAttemptAt.isAfter(now);
        }
        return status == NotificationDeliveryStatus.SENDING
                && lastAttemptedAt != null
                && !lastAttemptedAt.isAfter(now.minus(sendingTimeout));
    }

    public void claim(Instant claimedAt) {
        status = NotificationDeliveryStatus.SENDING;
        attemptCount++;
        lastAttemptedAt = claimedAt;
        updatedAt = claimedAt;
    }

    public void failWithoutAttempt(String errorCode, Instant failedAt) {
        status = NotificationDeliveryStatus.FAILED;
        lastErrorCode = errorCode;
        updatedAt = failedAt;
    }

    public void markSent(String providerMessageId, Instant sentAt) {
        if (status != NotificationDeliveryStatus.SENDING) {
            return;
        }
        status = NotificationDeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.sentAt = sentAt;
        this.lastErrorCode = null;
        this.updatedAt = sentAt;
    }

    public void handleFailure(
            String errorCode,
            boolean retryable,
            boolean invalidRegistration,
            Instant failedAt,
            int maxAttempts,
            Duration initialRetryDelay,
            Duration maxRetryDelay
    ) {
        if (status != NotificationDeliveryStatus.SENDING) {
            return;
        }
        this.lastErrorCode = errorCode;
        if (invalidRegistration) {
            pushDevice.invalidate(failedAt);
        }
        if (!retryable || attemptCount >= maxAttempts) {
            status = NotificationDeliveryStatus.FAILED;
            updatedAt = failedAt;
            return;
        }
        status = NotificationDeliveryStatus.RETRY_PENDING;
        nextAttemptAt = failedAt.plus(retryDelay(
                initialRetryDelay,
                maxRetryDelay
        ));
        updatedAt = failedAt;
    }

    public String getEncryptedRegistrationId() {
        return pushDevice.getEncryptedRegistrationId();
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public PushProviderType getProvider() {
        return provider;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public PushDeviceStatus getPushDeviceStatus() {
        return pushDevice.getStatus();
    }

    public Notification getNotification() {
        return notification;
    }

    private Duration retryDelay(
            Duration initialRetryDelay,
            Duration maxRetryDelay
    ) {
        long multiplier = 1L << Math.min(attemptCount - 1, 30);
        try {
            Duration delay = initialRetryDelay.multipliedBy(multiplier);
            return delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay;
        } catch (ArithmeticException exception) {
            return maxRetryDelay;
        }
    }
}
