package kr.omong.dulpick.domain.feedback.application;

import kr.omong.dulpick.domain.feedback.domain.FeedbackStatus;

import java.time.Instant;

public record ReceivedFeedback(
        Long feedbackId,
        FeedbackStatus status,
        Instant createdAt
) {
}
