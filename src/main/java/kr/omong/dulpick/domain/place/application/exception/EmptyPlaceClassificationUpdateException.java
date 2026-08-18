package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class EmptyPlaceClassificationUpdateException extends BusinessException {

    public EmptyPlaceClassificationUpdateException() {
        super(ErrorCode.INVALID_INPUT);
    }
}
