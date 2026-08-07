package kr.omong.dulpick.domain.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsCommand;

public record NotificationSettingsRequest(
        @NotNull
        @Schema(description = "콘텐츠 저장 푸시 수신 여부")
        Boolean contentSavedEnabled,
        @NotNull
        @Schema(description = "데이트 일정 푸시 수신 여부")
        Boolean dateScheduleEnabled,
        @NotNull
        @Schema(description = "마케팅 알림함·푸시 수신 동의 여부")
        Boolean marketingEnabled,
        @Schema(description = "마케팅 수신 동의 버전. 마케팅 알림을 켤 때 필수")
        String marketingConsentVersion
) {

    public NotificationSettingsCommand toCommand() {
        return new NotificationSettingsCommand(
                contentSavedEnabled,
                dateScheduleEnabled,
                marketingEnabled,
                marketingConsentVersion
        );
    }
}
