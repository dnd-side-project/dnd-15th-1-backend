package kr.omong.dulpick.domain.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationCommand;

public record MarketingNotificationRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "푸시 알림 제목", example = "둘픽의 새로운 장소를 확인해 보세요")
        String title,
        @NotBlank
        @Size(max = 500)
        @Schema(description = "푸시 알림 본문", example = "이번 주말 데이트 장소를 둘픽에서 찾아보세요.")
        String body
) {

    public MarketingNotificationCommand toCommand() {
        return new MarketingNotificationCommand(title.strip(), body.strip());
    }
}
