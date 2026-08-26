package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PlaceVerificationUnavailableException extends BusinessException {

    public PlaceVerificationUnavailableException(Throwable cause) {
        super(ErrorCode.PLACE_VERIFICATION_UNAVAILABLE, cause);
    }

    public PlaceVerificationUnavailableException() {
        super(ErrorCode.PLACE_VERIFICATION_UNAVAILABLE);
    }
}
