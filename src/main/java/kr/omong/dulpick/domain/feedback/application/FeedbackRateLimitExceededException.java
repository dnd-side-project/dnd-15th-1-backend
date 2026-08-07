package kr.omong.dulpick.domain.feedback.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class FeedbackRateLimitExceededException extends BusinessException {

    public FeedbackRateLimitExceededException() {
        super(ErrorCode.FEEDBACK_RATE_LIMIT_EXCEEDED);
    }
}
