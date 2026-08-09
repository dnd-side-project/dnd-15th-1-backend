package kr.omong.dulpick.domain.notification.application.command;

import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;

import java.time.Instant;
import java.util.UUID;

public record PushDeviceView(
        UUID deviceId,
        PushDeviceStatus status,
        Instant registeredAt
) {
}
