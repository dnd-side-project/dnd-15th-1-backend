package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class WalkingRouteUnavailableException extends BusinessException {

    public WalkingRouteUnavailableException() {
        super(ErrorCode.WALKING_ROUTE_UNAVAILABLE);
    }
}
