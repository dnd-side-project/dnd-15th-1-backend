package kr.omong.dulpick.domain.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이메일 공지 수신 거부 등록 요청")
public record AddEmailOptOutRequest(
        @NotNull @Schema(example = "101", requiredMode = Schema.RequiredMode.REQUIRED) Long memberId
) {
}
