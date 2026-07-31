package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidSocialTokenException extends BusinessException {

    public InvalidSocialTokenException(Throwable cause) {
        super(ErrorCode.OAUTH_VERIFICATION_FAILED, cause);
    }

    public InvalidSocialTokenException() {
        super(ErrorCode.OAUTH_VERIFICATION_FAILED);
    }
}
