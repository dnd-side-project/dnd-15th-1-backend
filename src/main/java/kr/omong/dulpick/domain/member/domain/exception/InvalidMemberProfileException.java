package kr.omong.dulpick.domain.member.domain.exception;

import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.exception.FieldValidationException;

public class InvalidMemberProfileException extends FieldValidationException {

    public InvalidMemberProfileException() {
        this("profile", "INVALID_PROFILE", "프로필 입력값이 올바르지 않습니다");
    }

    public InvalidMemberProfileException(String field, String reason, String message) {
        super(ErrorCode.INVALID_INPUT, field, reason, message);
    }
}
