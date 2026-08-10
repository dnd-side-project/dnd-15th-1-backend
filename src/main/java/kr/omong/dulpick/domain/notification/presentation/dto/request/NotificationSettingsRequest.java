package kr.omong.dulpick.domain.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsCommand;

public record NotificationSettingsRequest(
        @NotNull
        @Schema(description = "상대방의 저장 콘텐츠 누적 알림을 푸시로 받을지 여부")
        Boolean contentSavedEnabled,
        @NotNull
        @Schema(description = "데이트 일정 시작 전 알림을 푸시로 받을지 여부")
        Boolean dateScheduleEnabled,
        @NotNull
        @Schema(description = "마케팅 알림을 알림함과 푸시로 받을지에 대한 동의 여부")
        Boolean marketingEnabled,
        @Schema(
                description = "마케팅 수신 동의 버전. marketingEnabled가 true이면 설정 조회 응답의 "
                        + "availableMarketingConsentVersion을 전달해야 하며, false이면 생략할 수 있습니다."
        )
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
