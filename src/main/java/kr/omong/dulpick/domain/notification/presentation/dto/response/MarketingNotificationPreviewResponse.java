package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationPreviewView;

import java.time.Instant;

public record MarketingNotificationPreviewResponse(
        @Schema(example = "42")
        int targetCount,
        Instant calculatedAt
) {
    public static MarketingNotificationPreviewResponse from(MarketingNotificationPreviewView view) {
        return new MarketingNotificationPreviewResponse(view.targetCount(), view.calculatedAt());
    }
}
