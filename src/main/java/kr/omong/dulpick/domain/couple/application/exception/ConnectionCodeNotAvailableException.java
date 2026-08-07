package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class ConnectionCodeNotAvailableException extends BusinessException {

    public ConnectionCodeNotAvailableException() {
        super(ErrorCode.CONNECTION_CODE_NOT_AVAILABLE);
    }
}
