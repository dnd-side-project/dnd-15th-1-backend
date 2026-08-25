package kr.omong.dulpick.domain.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationHistoryView;

import java.util.List;

public record MarketingNotificationHistoryResponse(
        List<MarketingNotificationResponse> campaigns,
        @Schema(example = "0")
        int page,
        @Schema(example = "10")
        int size,
        @Schema(example = "12")
        long totalElements,
        @Schema(example = "2")
        int totalPages,
        @Schema(example = "true")
        boolean hasNext
) {
    public static MarketingNotificationHistoryResponse from(MarketingNotificationHistoryView view) {
        return new MarketingNotificationHistoryResponse(
                view.campaigns().stream().map(MarketingNotificationResponse::from).toList(),
                view.page(), view.size(), view.totalElements(), view.totalPages(), view.hasNext()
        );
    }
}
