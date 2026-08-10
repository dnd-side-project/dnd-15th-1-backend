package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.query.NotificationPageView;

import java.util.List;

public record NotificationPageResponse(
        @Schema(description = "현재 페이지의 알림 목록. 최신 알림부터 반환합니다.")
        List<NotificationResponse> notifications,
        @Schema(
                description = "다음 페이지 조회 토큰. 다음 요청의 cursor로 그대로 전달하며, "
                        + "마지막 페이지이면 null입니다.",
                nullable = true
        )
        String nextCursor,
        @Schema(description = "다음 페이지가 있으면 true")
        boolean hasNext,
        @Schema(description = "현재 페이지와 관계없는 내 전체 읽지 않은 알림 수")
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
