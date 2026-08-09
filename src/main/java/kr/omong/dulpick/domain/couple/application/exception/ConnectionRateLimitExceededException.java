package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class ConnectionRateLimitExceededException extends BusinessException {

    public ConnectionRateLimitExceededException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
