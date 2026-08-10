package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsView;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record NotificationSettingsResponse(
        @Schema(description = "상대방의 저장 콘텐츠 누적 알림 푸시 수신 여부")
        boolean contentSavedEnabled,
        @Schema(description = "데이트 일정 시작 전 알림 푸시 수신 여부")
        boolean dateScheduleEnabled,
        @Schema(description = "마케팅 알림함·푸시 수신 동의 여부")
        boolean marketingEnabled,
        @Schema(description = "현재 동의한 마케팅 약관 버전. 마케팅 수신에 동의하지 않았으면 null")
        String marketingConsentVersion,
        @Schema(description = "마케팅 알림을 켤 때 marketingConsentVersion으로 전달해야 하는 최신 약관 버전")
        String availableMarketingConsentVersion,
        @Schema(description = "알림 설정이 생성되거나 마지막으로 변경된 시각")
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
