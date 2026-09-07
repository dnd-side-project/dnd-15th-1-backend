package kr.omong.dulpick.domain.notice.application;

public record NoticeCreateView(
        NoticeView notice,
        NoticeNotificationView notification
) {
}
