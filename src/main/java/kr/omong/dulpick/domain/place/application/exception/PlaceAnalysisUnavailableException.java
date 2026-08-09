package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PlaceAnalysisUnavailableException extends BusinessException {

    public PlaceAnalysisUnavailableException(Throwable cause) {
        super(ErrorCode.PLACE_ANALYSIS_UNAVAILABLE, cause);
    }
}
