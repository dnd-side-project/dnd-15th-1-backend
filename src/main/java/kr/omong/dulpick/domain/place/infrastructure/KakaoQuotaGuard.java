package kr.omong.dulpick.domain.place.infrastructure;

import org.slf4j.Logger;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Kakao 무료 쿼터가 소진되면(HTTP 429 또는 400 + code -10) 일정 시간 호출을 쉬어
 * 실패할 요청을 반복하지 않도록 합니다.
 */
final class KakaoQuotaGuard {

    private static final Duration COOLDOWN = Duration.ofMinutes(10);
    private static final String QUOTA_EXCEEDED_CODE = "\"code\":-10";

    private final Clock clock;
    private volatile Instant blockedUntil = Instant.MIN;

    KakaoQuotaGuard(Clock clock) {
        this.clock = clock;
    }

    boolean isBlocked() {
        return clock.instant().isBefore(blockedUntil);
    }

    /**
     * 쿼터 소진 응답이면 쿨다운을 시작하고 true를 돌려줍니다.
     */
    boolean recordIfQuotaExceeded(RestClientResponseException exception, String api, Logger logger) {
        if (!isQuotaExceeded(exception)) {
            return false;
        }
        boolean alreadyBlocked = isBlocked();
        blockedUntil = clock.instant().plus(COOLDOWN);
        if (!alreadyBlocked) {
            logger.warn(
                    "Kakao {} quota exhausted (status={}); skipping calls for {} minutes",
                    api,
                    exception.getStatusCode().value(),
                    COOLDOWN.toMinutes()
            );
        }
        return true;
    }

    private boolean isQuotaExceeded(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 429) {
            return true;
        }
        if (status != 400) {
            return false;
        }
        String body = exception.getResponseBodyAsString().replace(" ", "");
        return body.contains(QUOTA_EXCEEDED_CODE);
    }
}
