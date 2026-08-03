package kr.omong.dulpick.domain.member.domain.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MemberNotActiveException extends BusinessException {

    public MemberNotActiveException() {
        super(ErrorCode.MEMBER_WITHDRAWN);
    }
}
