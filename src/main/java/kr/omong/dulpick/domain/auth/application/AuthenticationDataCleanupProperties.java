package kr.omong.dulpick.domain.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("auth.maintenance")
public record AuthenticationDataCleanupProperties(
        Duration fixedDelay,
        int batchSize,
        int maxBatchesPerRun,
        Duration revokedRefreshTokenRetention
) {

    public AuthenticationDataCleanupProperties {
        if (fixedDelay == null || fixedDelay.isZero() || fixedDelay.isNegative()) {
            throw new IllegalArgumentException("auth.maintenance.fixed-delay must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("auth.maintenance.batch-size must be positive");
        }
        if (maxBatchesPerRun <= 0) {
            throw new IllegalArgumentException(
                    "auth.maintenance.max-batches-per-run must be positive"
            );
        }
        if (revokedRefreshTokenRetention == null
                || revokedRefreshTokenRetention.isNegative()) {
            throw new IllegalArgumentException(
                    "auth.maintenance.revoked-refresh-token-retention must not be negative"
            );
        }
    }
}
