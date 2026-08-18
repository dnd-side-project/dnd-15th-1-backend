package kr.omong.dulpick.domain.place.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class RegionTagNotFoundException extends BusinessException {

    public RegionTagNotFoundException() {
        super(ErrorCode.REGION_TAG_NOT_FOUND);
    }
}
