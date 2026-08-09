package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class ConnectionConflictException extends BusinessException {

    public ConnectionConflictException() {
        super(ErrorCode.CONNECTION_CONFLICT);
    }
}
