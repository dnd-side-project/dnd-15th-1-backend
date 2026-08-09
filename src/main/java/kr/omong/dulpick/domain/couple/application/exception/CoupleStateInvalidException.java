package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class CoupleStateInvalidException extends BusinessException {

    public CoupleStateInvalidException() {
        super(ErrorCode.COUPLE_STATE_INVALID);
    }
}
