package kr.omong.dulpick.domain.member.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MemberProfileRequiredException extends BusinessException {

    public MemberProfileRequiredException() {
        super(ErrorCode.PROFILE_REQUIRED);
    }
}
