package kr.omong.dulpick.domain.couple.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("couple.abuse")
public record ConnectionAbuseProperties(
        int previewPerMinute,
        int previewPerHour,
        int connectPerMinute,
        int connectPerDay,
        int stateChangesPerDay,
        int codeFailuresPerTenMinutes,
        int ipFailuresPerHour,
        String ipHashKey,
        Duration failureBlockDuration,
        Duration retention
) {

    public ConnectionAbuseProperties {
        if (previewPerMinute <= 0
                || previewPerHour <= 0
                || connectPerMinute <= 0
                || connectPerDay <= 0
                || stateChangesPerDay <= 0
                || codeFailuresPerTenMinutes <= 0
                || ipFailuresPerHour <= 0) {
            throw new IllegalArgumentException("couple abuse limits must be positive");
        }
        if (failureBlockDuration == null
                || failureBlockDuration.isZero()
                || failureBlockDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "couple.abuse.failure-block-duration must be positive"
            );
        }
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("couple.abuse.retention must be positive");
        }
        if (ipHashKey == null || ipHashKey.isBlank()) {
            throw new IllegalArgumentException(
                    "couple.abuse.ip-hash-key must not be blank"
            );
        }
    }
}
