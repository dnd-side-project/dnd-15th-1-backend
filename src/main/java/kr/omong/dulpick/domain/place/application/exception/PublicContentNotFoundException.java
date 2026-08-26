package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PublicContentNotFoundException extends BusinessException {

    public PublicContentNotFoundException() {
        super(ErrorCode.PUBLIC_CONTENT_NOT_FOUND);
    }
}
