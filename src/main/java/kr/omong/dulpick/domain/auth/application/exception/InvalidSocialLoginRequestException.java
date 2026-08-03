package kr.omong.dulpick.domain.auth.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidSocialLoginRequestException extends BusinessException {

    public InvalidSocialLoginRequestException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }
}
