package kr.omong.dulpick.domain.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.notification.application.command.RegisterPushDeviceCommand;
import kr.omong.dulpick.domain.notification.domain.PushPlatform;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;

import java.util.UUID;

public record PushDeviceRequest(
        @NotNull
        @Schema(description = "플랫폼", allowableValues = {"IOS"})
        PushPlatform platform,
        @NotNull
        @Schema(description = "푸시 공급자", allowableValues = {"FCM"})
        PushProviderType provider,
        @NotBlank
        @Size(max = 2_000)
        @Schema(description = "Firebase SDK가 발급한 FCM 등록 토큰", writeOnly = true)
        String providerRegistrationId,
        @Size(max = 30)
        @Schema(description = "iOS 앱 버전", example = "1.0.0")
        String appVersion
) {

    public RegisterPushDeviceCommand toCommand(UUID deviceId) {
        return new RegisterPushDeviceCommand(
                deviceId,
                platform,
                provider,
                providerRegistrationId,
                appVersion
        );
    }
}
