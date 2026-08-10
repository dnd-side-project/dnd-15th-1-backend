package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceView;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;
import java.util.UUID;

public record PushDeviceResponse(
        @Schema(description = "요청 경로로 전달한 앱 설치 단위 디바이스 UUID")
        UUID deviceId,
        @Schema(description = "푸시 발송 대상 등록 상태", allowableValues = {"ACTIVE"})
        PushDeviceStatus status,
        @Schema(description = "FCM 토큰을 마지막으로 등록하거나 갱신한 시각")
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
