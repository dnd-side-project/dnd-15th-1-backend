package kr.omong.dulpick.domain.auth.infrastructure.apple;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class AppleAuthorizationException extends BusinessException {

    public AppleAuthorizationException(String message) {
        super(ErrorCode.OAUTH_VERIFICATION_FAILED, message);
    }

    public AppleAuthorizationException(String message, Throwable cause) {
        super(ErrorCode.OAUTH_VERIFICATION_FAILED, message, cause);
    }
}
