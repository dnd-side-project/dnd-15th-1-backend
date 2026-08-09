package kr.omong.dulpick.domain.notification.domain;

public enum NotificationDeliveryStatus {
    PENDING,
    SENDING,
    RETRY_PENDING,
    SENT,
    FAILED
}
