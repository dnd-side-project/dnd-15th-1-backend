package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class AppleTokenRevocationException extends BusinessException {

    public AppleTokenRevocationException(Throwable cause) {
        super(ErrorCode.APPLE_TOKEN_REVOCATION_FAILED, cause);
    }
}
