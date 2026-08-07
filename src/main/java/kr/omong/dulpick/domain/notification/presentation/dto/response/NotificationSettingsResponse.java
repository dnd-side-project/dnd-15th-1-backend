package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsView;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record NotificationSettingsResponse(
        @Schema(description = "콘텐츠 저장 푸시 수신 여부")
        boolean contentSavedEnabled,
        @Schema(description = "데이트 일정 푸시 수신 여부")
        boolean dateScheduleEnabled,
        @Schema(description = "마케팅 알림함·푸시 수신 동의 여부")
        boolean marketingEnabled,
        @Schema(description = "현재 회원의 마케팅 수신 동의 버전. 미동의 시 null")
        String marketingConsentVersion,
        @Schema(description = "마케팅 알림을 켤 때 요청해야 하는 최신 동의 버전")
        String availableMarketingConsentVersion,
        @Schema(description = "마지막 변경 시각")
        LocalDateTime updatedAt
) {

    public static NotificationSettingsResponse from(NotificationSettingsView settings) {
        return new NotificationSettingsResponse(
                settings.contentSavedEnabled(),
                settings.dateScheduleEnabled(),
                settings.marketingEnabled(),
                settings.marketingConsentVersion(),
                settings.availableMarketingConsentVersion(),
                ServiceTime.toLocalDateTime(settings.updatedAt())
        );
    }
}
