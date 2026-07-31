package kr.omong.dulpick.domain.testauth.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class TestAuthEmailAlreadyExistsException extends BusinessException {

    public TestAuthEmailAlreadyExistsException() {
        super(ErrorCode.INVALID_INPUT, "Test authentication email already exists");
    }

    public TestAuthEmailAlreadyExistsException(Throwable cause) {
        super(ErrorCode.INVALID_INPUT, "Test authentication email already exists", cause);
    }
}
