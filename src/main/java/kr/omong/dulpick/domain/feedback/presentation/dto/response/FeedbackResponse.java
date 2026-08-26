package kr.omong.dulpick.domain.feedback.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.feedback.application.ReceivedFeedback;
import kr.omong.dulpick.domain.feedback.domain.FeedbackStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record FeedbackResponse(
        @Schema(description = "피드백 접수 ID", example = "501", requiredMode = Schema.RequiredMode.REQUIRED)
        Long feedbackId,
        @Schema(description = "피드백 처리 상태입니다.", allowableValues = {"RECEIVED", "IN_REVIEW", "RESOLVED"}, example = "RECEIVED", requiredMode = Schema.RequiredMode.REQUIRED)
        FeedbackStatus status,
        @Schema(description = "피드백 접수 시각", example = "2026-08-16T14:30:00", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt
) {

    public static FeedbackResponse from(ReceivedFeedback feedback) {
        return new FeedbackResponse(
                feedback.feedbackId(),
                feedback.status(),
                ServiceTime.toLocalDateTime(feedback.createdAt())
        );
    }
}
