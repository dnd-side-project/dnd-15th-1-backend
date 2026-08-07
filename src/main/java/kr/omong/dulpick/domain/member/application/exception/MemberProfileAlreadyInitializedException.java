package kr.omong.dulpick.domain.member.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MemberProfileAlreadyInitializedException extends BusinessException {

    public MemberProfileAlreadyInitializedException() {
        super(ErrorCode.PROFILE_ALREADY_INITIALIZED);
    }
}
