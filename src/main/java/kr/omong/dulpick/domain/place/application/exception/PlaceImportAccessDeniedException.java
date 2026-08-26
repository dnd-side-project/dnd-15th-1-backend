package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PlaceImportAccessDeniedException extends BusinessException {

    public PlaceImportAccessDeniedException() {
        super(ErrorCode.PLACE_IMPORT_ACCESS_DENIED);
    }
}
