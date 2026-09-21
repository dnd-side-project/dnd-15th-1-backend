package kr.omong.dulpick.domain.notice.application;

public record NoticeCommand(
        String title,
        String content,
        boolean sendNotification
) {
}
