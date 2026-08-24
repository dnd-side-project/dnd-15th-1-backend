package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PublicContentImageUnavailableException extends BusinessException {

    public PublicContentImageUnavailableException() {
        super(ErrorCode.PUBLIC_CONTENT_IMAGE_UNAVAILABLE);
    }

    public PublicContentImageUnavailableException(Throwable cause) {
        super(ErrorCode.PUBLIC_CONTENT_IMAGE_UNAVAILABLE, cause);
    }
}
