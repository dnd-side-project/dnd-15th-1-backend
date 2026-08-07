package kr.omong.dulpick.global.exception;

import java.util.List;

public class FieldValidationException extends BusinessException {

    private final List<FieldErrorResponse> fieldErrors;

    public FieldValidationException(
            ErrorCode errorCode,
            String field,
            String reason,
            String message
    ) {
        super(errorCode);
        this.fieldErrors = List.of(new FieldErrorResponse(field, reason, message));
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }
}
