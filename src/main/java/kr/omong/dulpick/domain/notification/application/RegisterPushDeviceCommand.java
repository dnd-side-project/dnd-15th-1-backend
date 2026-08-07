package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.notification.domain.PushPlatform;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;

import java.util.UUID;

public record RegisterPushDeviceCommand(
        UUID deviceId,
        PushPlatform platform,
        PushProviderType provider,
        String providerRegistrationId,
        String appVersion
) {
}
