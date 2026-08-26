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
        @Schema(description = "현재 지원하는 앱 플랫폼. IOS만 전달할 수 있습니다.", allowableValues = {"IOS"}, example = "IOS")
        PushPlatform platform,
        @NotNull
        @Schema(description = "현재 사용하는 푸시 공급자. FCM만 전달할 수 있습니다.", allowableValues = {"FCM"}, example = "FCM")
        PushProviderType provider,
        @NotBlank
        @Size(max = 2_000)
        @Schema(
                description = "Firebase SDK가 현재 앱 설치에 발급한 FCM 등록 토큰. "
                        + "토큰이 갱신되면 이 API를 다시 호출해야 합니다.",
                example = "fcm-registration-token-example",
                writeOnly = true
        )
        String providerRegistrationId,
        @Size(max = 30)
        @Schema(description = "현재 설치된 iOS 앱 버전", example = "1.0.0")
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
