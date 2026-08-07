package kr.omong.dulpick.domain.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.NotificationPageView;

import java.util.List;

public record NotificationPageResponse(
        @Schema(description = "최신순 알림 목록")
        List<NotificationResponse> notifications,
        @Schema(description = "다음 페이지 커서", nullable = true)
        String nextCursor,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,
        @Schema(description = "현재 회원의 전체 미확인 알림 수")
        long unreadCount
) {

    public static NotificationPageResponse from(NotificationPageView page) {
        return new NotificationPageResponse(
                page.notifications().stream().map(NotificationResponse::from).toList(),
                page.nextCursor(),
                page.hasNext(),
                page.unreadCount()
        );
    }
}
