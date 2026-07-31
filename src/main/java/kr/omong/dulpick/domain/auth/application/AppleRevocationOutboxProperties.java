package kr.omong.dulpick.domain.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("auth.apple.revocation")
public record AppleRevocationOutboxProperties(
        Duration processDelay,
        int batchSize,
        Duration initialRetryDelay,
        Duration maxRetryDelay
) {

    public AppleRevocationOutboxProperties {
        requirePositive(processDelay, "process-delay");
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "auth.apple.revocation.batch-size must be positive"
            );
        }
        requirePositive(initialRetryDelay, "initial-retry-delay");
        requirePositive(maxRetryDelay, "max-retry-delay");
        if (initialRetryDelay.compareTo(maxRetryDelay) > 0) {
            throw new IllegalArgumentException(
                    "auth.apple.revocation.initial-retry-delay must not exceed max-retry-delay"
            );
        }
    }

    private static void requirePositive(Duration duration, String propertyName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "auth.apple.revocation." + propertyName + " must be positive"
            );
        }
    }
}
