package kr.omong.dulpick.domain.feedback.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.feedback.application.FeedbackCommand;
import kr.omong.dulpick.domain.feedback.domain.FeedbackType;

import java.util.UUID;

public record FeedbackRequest(
        @NotNull
        @Schema(description = "중복 접수 방지를 위해 iOS가 생성한 요청 UUID")
        UUID clientRequestId,
        @NotNull
        @Schema(
                description = "피드백 유형",
                allowableValues = {"INQUIRY", "BUG_REPORT", "FEATURE_SUGGESTION", "OTHER"}
        )
        FeedbackType type,
        @NotBlank
        @Size(max = 1_000)
        @Schema(description = "피드백 내용. 앞뒤 공백을 제외한 1~1,000자")
        String content
) {

    public FeedbackCommand toCommand() {
        return new FeedbackCommand(clientRequestId, type, content);
    }
}
