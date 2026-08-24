package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationSendView;

import java.time.Instant;

public record MarketingNotificationResponse(
        @Schema(description = "운영자 발송 작업 식별자")
        String campaignId,
        @Schema(description = "발송 대상이 된 마케팅 수신 동의 활성 회원 수")
        int targetCount,
        @Schema(description = "알림함 저장 및 푸시 큐 등록 시각")
        Instant queuedAt
) {

    public static MarketingNotificationResponse from(MarketingNotificationSendView view) {
        return new MarketingNotificationResponse(
                view.campaignId(),
                view.targetCount(),
                view.queuedAt()
        );
    }
}
