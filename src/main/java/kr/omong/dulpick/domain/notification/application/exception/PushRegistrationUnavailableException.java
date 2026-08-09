package kr.omong.dulpick.domain.notification.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PushRegistrationUnavailableException extends BusinessException {

    public PushRegistrationUnavailableException(Throwable cause) {
        super(ErrorCode.PUSH_REGISTRATION_UNAVAILABLE, cause);
    }
}
