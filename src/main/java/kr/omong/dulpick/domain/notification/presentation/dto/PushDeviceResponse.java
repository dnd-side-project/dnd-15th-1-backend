package kr.omong.dulpick.domain.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.PushDeviceView;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;
import java.util.UUID;

public record PushDeviceResponse(
        @Schema(description = "iOS 앱 설치 단위 디바이스 ID")
        UUID deviceId,
        @Schema(description = "등록 상태", allowableValues = {"ACTIVE"})
        PushDeviceStatus status,
        @Schema(description = "마지막 등록·갱신 시각")
        LocalDateTime registeredAt
) {

    public static PushDeviceResponse from(PushDeviceView device) {
        return new PushDeviceResponse(
                device.deviceId(),
                device.status(),
                ServiceTime.toLocalDateTime(device.registeredAt())
        );
    }
}
