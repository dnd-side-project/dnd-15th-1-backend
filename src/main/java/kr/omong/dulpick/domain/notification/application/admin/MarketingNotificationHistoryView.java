package kr.omong.dulpick.domain.notification.application.admin;

import java.util.List;

public record MarketingNotificationHistoryView(
        List<MarketingNotificationSendView> campaigns,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
