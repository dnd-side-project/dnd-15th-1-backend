package kr.omong.dulpick.domain.notification.application.admin;

public record MarketingNotificationCommand(
        String title,
        String body
) {
}
