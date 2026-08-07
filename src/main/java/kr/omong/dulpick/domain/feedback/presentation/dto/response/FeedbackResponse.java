package kr.omong.dulpick.domain.feedback.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.feedback.application.ReceivedFeedback;
import kr.omong.dulpick.domain.feedback.domain.FeedbackStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record FeedbackResponse(
        @Schema(description = "피드백 접수 ID")
        Long feedbackId,
        @Schema(description = "처리 상태", allowableValues = {"RECEIVED", "IN_REVIEW", "RESOLVED"})
        FeedbackStatus status,
        @Schema(description = "접수 시각")
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
