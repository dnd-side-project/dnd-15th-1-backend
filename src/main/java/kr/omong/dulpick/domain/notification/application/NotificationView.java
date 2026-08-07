package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;

import java.time.Instant;

public record NotificationView(
        Long id,
        NotificationType type,
        String title,
        String body,
        NotificationRoute route,
        String referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
