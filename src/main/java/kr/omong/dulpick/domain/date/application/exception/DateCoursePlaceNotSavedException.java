package kr.omong.dulpick.domain.date.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class DateCoursePlaceNotSavedException extends BusinessException {

    public DateCoursePlaceNotSavedException() {
        super(ErrorCode.DATE_COURSE_PLACE_NOT_SAVED);
    }
}
