package kr.omong.dulpick.domain.notification.application.query;

import java.util.List;

public record NotificationPageView(
        List<NotificationView> notifications,
        String nextCursor,
        boolean hasNext,
        long unreadCount
) {
}
