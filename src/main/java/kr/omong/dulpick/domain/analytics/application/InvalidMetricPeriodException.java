package kr.omong.dulpick.domain.analytics.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class InvalidMetricPeriodException extends BusinessException {

    public InvalidMetricPeriodException() {
        super(ErrorCode.INVALID_INPUT, "성과 지표 조회 기간이 올바르지 않습니다");
    }
}
