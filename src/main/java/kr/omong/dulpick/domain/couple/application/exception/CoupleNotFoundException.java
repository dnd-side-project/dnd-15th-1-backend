package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class CoupleNotFoundException extends BusinessException {

    public CoupleNotFoundException() {
        super(ErrorCode.COUPLE_NOT_FOUND);
    }
}
