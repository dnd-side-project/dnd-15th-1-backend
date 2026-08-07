package kr.omong.dulpick.domain.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.NotificationView;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(description = "알림 ID")
        Long id,
        @Schema(description = "알림 유형")
        NotificationType type,
        @Schema(description = "알림 제목")
        String title,
        @Schema(description = "알림 본문")
        String body,
        @Schema(description = "알림 선택 시 이동할 앱 화면 코드")
        NotificationRoute route,
        @Schema(description = "이동 대상 식별자", nullable = true)
        String referenceId,
        @Schema(description = "확인 여부")
        boolean read,
        @Schema(description = "확인 시각", nullable = true)
        LocalDateTime readAt,
        @Schema(description = "알림 생성 시각")
        LocalDateTime createdAt
) {

    public static NotificationResponse from(NotificationView view) {
        return new NotificationResponse(
                view.id(),
                view.type(),
                view.title(),
                view.body(),
                view.route(),
                view.referenceId(),
                view.read(),
                ServiceTime.toLocalDateTime(view.readAt()),
                ServiceTime.toLocalDateTime(view.createdAt())
        );
    }
}
