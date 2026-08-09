package kr.omong.dulpick.domain.notification.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class MarketingConsentVersionOutdatedException extends BusinessException {

    public MarketingConsentVersionOutdatedException() {
        super(ErrorCode.MARKETING_CONSENT_VERSION_OUTDATED);
    }
}
