package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PlaceSaveNotFoundException extends BusinessException {

    public PlaceSaveNotFoundException() {
        super(ErrorCode.PLACE_SAVE_NOT_FOUND);
    }
}
