package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PushDeviceConflictException extends BusinessException {

    public PushDeviceConflictException(Throwable cause) {
        super(ErrorCode.PUSH_DEVICE_CONFLICT, cause);
    }
}
