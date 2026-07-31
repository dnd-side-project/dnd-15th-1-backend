package kr.omong.dulpick.domain.testauth.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class TestAuthAuthenticationException extends BusinessException {

    public TestAuthAuthenticationException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }
}
