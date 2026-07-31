package kr.omong.dulpick.domain.member.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MemberAlreadyWithdrawnException extends BusinessException {

    public MemberAlreadyWithdrawnException() {
        super(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
    }
}
