package kr.omong.dulpick.domain.couple.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MemberAlreadyConnectedException extends BusinessException {

    public MemberAlreadyConnectedException() {
        super(ErrorCode.MEMBER_ALREADY_CONNECTED);
    }
}
