package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MetadataUnavailableException extends BusinessException {

    public MetadataUnavailableException(Throwable cause) {
        super(ErrorCode.PLACE_METADATA_UNAVAILABLE, cause);
    }

    public MetadataUnavailableException() {
        super(ErrorCode.PLACE_METADATA_UNAVAILABLE);
    }
}
