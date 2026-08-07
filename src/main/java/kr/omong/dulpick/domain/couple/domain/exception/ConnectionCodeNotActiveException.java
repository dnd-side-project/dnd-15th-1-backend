package kr.omong.dulpick.domain.couple.domain.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class ConnectionCodeNotActiveException extends BusinessException {

    public ConnectionCodeNotActiveException() {
        super(ErrorCode.CONNECTION_CONFLICT);
    }
}
