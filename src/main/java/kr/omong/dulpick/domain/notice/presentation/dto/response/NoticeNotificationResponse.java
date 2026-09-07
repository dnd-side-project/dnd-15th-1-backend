package kr.omong.dulpick.domain.notice.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notice.application.NoticeNotificationView;

import java.time.Instant;

public record NoticeNotificationResponse(
        @Schema(description = "알림 작업 식별자") String campaignId,
        @Schema(description = "알림 작업 상태", example = "PENDING") String status,
        @Schema(description = "발송 대상 활성 회원 수", example = "42") int targetCount,
        @Schema(description = "현재까지 알림함·푸시 큐 등록이 완료된 회원 수", example = "0") int queuedCount,
        @Schema(description = "알림 작업 등록 시각") Instant queuedAt
) {

    public static NoticeNotificationResponse from(NoticeNotificationView view) {
        return new NoticeNotificationResponse(
                view.campaignId(),
                view.status(),
                view.targetCount(),
                view.queuedCount(),
                view.queuedAt()
        );
    }
}
