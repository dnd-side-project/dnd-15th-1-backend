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
        @Schema(
                description = """
                        iOS가 피드백 전송 전에 생성하는 요청 UUID입니다.
                        동일한 피드백 전송을 재시도할 때는 같은 UUID를 사용하고,
                        새로운 피드백에는 새로운 UUID를 생성합니다.
                        """,
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        UUID clientRequestId,
        @NotNull
        @Schema(
                description = "피드백 유형",
                allowableValues = {"INQUIRY", "BUG_REPORT", "FEATURE_SUGGESTION", "OTHER"},
                example = "FEATURE_SUGGESTION"
        )
        FeedbackType type,
        @NotBlank
        @Size(max = 1_000)
        @Schema(description = "피드백 내용. 앞뒤 공백을 제외한 1~1,000자", example = "장소 목록에서 지역별 필터를 제공해주세요")
        String content
) {

    public FeedbackCommand toCommand() {
        return new FeedbackCommand(clientRequestId, type, content);
    }
}
