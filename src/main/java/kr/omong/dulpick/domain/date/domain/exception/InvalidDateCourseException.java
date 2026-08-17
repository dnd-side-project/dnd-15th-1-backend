package kr.omong.dulpick.domain.date.domain.exception;

import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.exception.FieldValidationException;

public class InvalidDateCourseException extends FieldValidationException {

    public InvalidDateCourseException(String field, String reason, String message) {
        super(ErrorCode.INVALID_INPUT, field, reason, message);
    }
}
