package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PlaceImportNotFoundException extends BusinessException {

    public PlaceImportNotFoundException() {
        super(ErrorCode.PLACE_IMPORT_NOT_FOUND);
    }
}
