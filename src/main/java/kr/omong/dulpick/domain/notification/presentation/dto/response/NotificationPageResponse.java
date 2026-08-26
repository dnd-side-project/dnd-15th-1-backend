package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.query.NotificationPageView;

import java.util.List;

public record NotificationPageResponse(
        @Schema(description = "현재 페이지의 알림 목록입니다. 최신 알림부터 반환하며 없으면 빈 배열입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<NotificationResponse> notifications,
        @Schema(
                description = "다음 페이지 조회 토큰. 다음 요청의 cursor로 그대로 전달하며, "
                        + "마지막 페이지이면 null입니다.",
                example = "next-cursor-token",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String nextCursor,
        @Schema(description = "다음 페이지가 있으면 true입니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext,
        @Schema(description = "현재 페이지와 관계없이 현재 회원이 읽지 않은 전체 알림 수입니다.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
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
