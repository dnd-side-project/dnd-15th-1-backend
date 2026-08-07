package kr.omong.dulpick.domain.notification.application.command;

public record NotificationSettingsCommand(
        boolean contentSavedEnabled,
        boolean dateScheduleEnabled,
        boolean marketingEnabled,
        String marketingConsentVersion
) {
}
