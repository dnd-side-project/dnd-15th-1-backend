package kr.omong.dulpick.domain.notification.application.command;

import java.time.Instant;

public record NotificationSettingsView(
        boolean contentSavedEnabled,
        boolean dateScheduleEnabled,
        boolean marketingEnabled,
        String marketingConsentVersion,
        String availableMarketingConsentVersion,
        Instant updatedAt
) {
}
