package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class UnsupportedSourceUrlException extends BusinessException {

    public UnsupportedSourceUrlException() {
        super(ErrorCode.UNSUPPORTED_SOURCE_URL);
    }
}
