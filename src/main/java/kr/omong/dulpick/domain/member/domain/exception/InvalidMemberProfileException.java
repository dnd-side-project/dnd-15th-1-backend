package kr.omong.dulpick.domain.member.domain.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidMemberProfileException extends BusinessException {

    public InvalidMemberProfileException() {
        super(ErrorCode.INVALID_INPUT);
    }
}
