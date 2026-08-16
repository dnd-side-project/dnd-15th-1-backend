package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.query.NotificationView;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record NotificationResponse(
        @Schema(description = "읽음 처리 API의 notificationId로 사용할 알림 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,
        @Schema(description = "알림 표시와 화면 분기에 사용할 알림 유형", requiredMode = Schema.RequiredMode.REQUIRED)
        NotificationType type,
        @Schema(description = "알림 제목", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @Schema(description = "알림 본문", requiredMode = Schema.RequiredMode.REQUIRED)
        String body,
        @Schema(description = "사용자가 알림을 선택했을 때 이동할 앱 화면 코드", requiredMode = Schema.RequiredMode.REQUIRED)
        NotificationRoute route,
        @Schema(description = "이동할 화면에서 사용할 대상 ID. 대상이 필요하지 않으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String referenceId,
        @Schema(description = "알림을 읽었으면 true입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean read,
        @Schema(description = "알림을 읽은 시각입니다. 읽지 않았으면 null입니다.", nullable = true, format = "date-time", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDateTime readAt,
        @Schema(description = "알림 생성 시각", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
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
