package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MarketingConsentVersionRequiredException extends BusinessException {

    public MarketingConsentVersionRequiredException() {
        super(ErrorCode.MARKETING_CONSENT_VERSION_REQUIRED);
    }
}
