package kr.omong.dulpick.domain.search.application.exception;

import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;

public class RecentSearchNotFoundException extends BusinessException {

    public RecentSearchNotFoundException() {
        super(ErrorCode.RECENT_SEARCH_NOT_FOUND);
    }
}
