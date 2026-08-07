package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidConnectionCodeException extends BusinessException {

    public InvalidConnectionCodeException() {
        super(ErrorCode.INVALID_CONNECTION_CODE);
    }
}
