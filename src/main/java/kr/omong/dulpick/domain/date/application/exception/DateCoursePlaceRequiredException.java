package kr.omong.dulpick.domain.date.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class DateCoursePlaceRequiredException extends BusinessException {

    public DateCoursePlaceRequiredException() {
        super(ErrorCode.DATE_COURSE_PLACE_REQUIRED);
    }
}
