package kr.omong.dulpick.domain.date.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class DateCourseNotFoundException extends BusinessException {

    public DateCourseNotFoundException() {
        super(ErrorCode.DATE_COURSE_NOT_FOUND);
    }
}
