package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class SelfConnectionNotAllowedException extends BusinessException {

    public SelfConnectionNotAllowedException() {
        super(ErrorCode.SELF_CONNECTION_NOT_ALLOWED);
    }
}
