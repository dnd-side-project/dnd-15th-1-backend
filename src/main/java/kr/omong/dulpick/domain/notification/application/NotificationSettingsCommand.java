package kr.omong.dulpick.domain.notification.application;

public record NotificationSettingsCommand(
        boolean contentSavedEnabled,
        boolean dateScheduleEnabled,
        boolean marketingEnabled,
        String marketingConsentVersion
) {
}
