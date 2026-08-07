package kr.omong.dulpick.domain.notification.infrastructure;

import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;

public record PushMessage(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        NotificationRoute route,
        String referenceId
) {
}
