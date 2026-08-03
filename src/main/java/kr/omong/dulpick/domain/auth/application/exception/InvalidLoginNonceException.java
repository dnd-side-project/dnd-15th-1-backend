package kr.omong.dulpick.domain.auth.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidLoginNonceException extends BusinessException {

    public InvalidLoginNonceException() {
        super(
                ErrorCode.OAUTH_VERIFICATION_FAILED,
                "Login nonce is invalid, expired, or already used"
        );
    }
}
