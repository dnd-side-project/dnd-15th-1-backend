package kr.omong.dulpick.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("notification.fcm")
public record FcmProperties(
        boolean enabled,
        String projectId,
        String credentialsPath,
        Duration processDelay,
        int batchSize,
        Duration initialRetryDelay,
        Duration maxRetryDelay,
        Duration sendingTimeout,
        int maxAttempts
) {

    public FcmProperties {
        requirePositive(processDelay, "process-delay");
        requirePositive(initialRetryDelay, "initial-retry-delay");
        requirePositive(maxRetryDelay, "max-retry-delay");
        requirePositive(sendingTimeout, "sending-timeout");
        if (batchSize <= 0 || maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "notification.fcm batch-size and max-attempts must be positive"
            );
        }
        if (initialRetryDelay.compareTo(maxRetryDelay) > 0) {
            throw new IllegalArgumentException(
                    "notification.fcm initial retry delay must not exceed max retry delay"
            );
        }
    }

    private static void requirePositive(Duration duration, String propertyName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "notification.fcm." + propertyName + " must be positive"
            );
        }
    }
}
