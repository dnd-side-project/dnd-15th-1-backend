package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class ConnectionCodeGenerationException extends BusinessException {

    public ConnectionCodeGenerationException() {
        super(ErrorCode.CONNECTION_CODE_GENERATION_FAILED);
    }
}
