package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationSendView;

import java.time.Instant;

public record MarketingNotificationResponse(
        @Schema(description = "운영자 발송 작업 식별자", example = "8f4d1f6f-4d9b-4f5e-9c53-7d2c0e4b7a11")
        String campaignId,
        @Schema(description = "발송 작업 상태", example = "PENDING")
        String status,
        @Schema(description = "발송 대상이 된 마케팅 수신 동의 활성 회원 수", example = "42")
        int targetCount,
        @Schema(description = "현재까지 알림함·푸시 큐 등록이 완료된 회원 수", example = "0")
        int queuedCount,
        @Schema(description = "알림함 저장 및 푸시 큐 등록 시각", example = "2026-08-24T12:00:00Z")
        Instant queuedAt
) {

    public static MarketingNotificationResponse from(MarketingNotificationSendView view) {
        return new MarketingNotificationResponse(
                view.campaignId(),
                view.status(),
                view.targetCount(),
                view.queuedCount(),
                view.queuedAt()
        );
    }
}
