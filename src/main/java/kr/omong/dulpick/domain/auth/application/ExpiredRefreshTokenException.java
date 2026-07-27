package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class ExpiredRefreshTokenException extends BusinessException {

    public ExpiredRefreshTokenException() {
        super(ErrorCode.EXPIRED_REFRESH_TOKEN);
    }
}
