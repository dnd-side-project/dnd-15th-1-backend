package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidPlaceCandidateException extends BusinessException {

    public InvalidPlaceCandidateException() {
        super(ErrorCode.PLACE_CANDIDATE_INVALID);
    }
}
