package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidSourceUrlException extends BusinessException {

    public InvalidSourceUrlException() {
        super(ErrorCode.INVALID_SOURCE_URL);
    }
}
