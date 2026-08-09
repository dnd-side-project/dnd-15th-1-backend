package kr.omong.dulpick.domain.notification.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class PushDeviceNotFoundException extends BusinessException {

    public PushDeviceNotFoundException() {
        super(ErrorCode.PUSH_DEVICE_NOT_FOUND);
    }
}
