package kr.omong.dulpick.domain.notification.application.support;

import kr.omong.dulpick.domain.notification.infrastructure.PushMessage;

public record NotificationDeliveryTask(
        Long deliveryId,
        String encryptedRegistrationId,
        PushMessage message
) {
}
