package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PlaceAlreadySavedException extends BusinessException {

    public PlaceAlreadySavedException() {
        super(ErrorCode.PLACE_ALREADY_SAVED);
    }
}
