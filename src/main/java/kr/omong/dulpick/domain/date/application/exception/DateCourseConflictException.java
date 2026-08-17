package kr.omong.dulpick.domain.date.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class DateCourseConflictException extends BusinessException {

    public DateCourseConflictException() {
        super(ErrorCode.DATE_COURSE_CONFLICT);
    }
}
