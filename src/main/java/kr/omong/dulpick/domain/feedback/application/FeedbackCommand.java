package kr.omong.dulpick.domain.feedback.application;

import kr.omong.dulpick.domain.feedback.domain.FeedbackType;

import java.util.UUID;

public record FeedbackCommand(
        UUID clientRequestId,
        FeedbackType type,
        String content
) {
}
