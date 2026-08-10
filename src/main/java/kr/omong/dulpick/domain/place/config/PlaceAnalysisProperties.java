package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("place-analysis")
public record PlaceAnalysisProperties(
        boolean enabled,
        int dailyLimit,
        int maxCandidates,
        int maxRetries,
        boolean publicCrawlerEnabled,
        int staleTimeoutSeconds,
        int retryCooldownSeconds,
        int maxRetryCount,
        Duration recoveryDelay,
        int recoveryBatchSize,
        int workerConcurrency
) {

    public PlaceAnalysisProperties {
        if (dailyLimit <= 0) {
            dailyLimit = 100;
        }
        if (maxCandidates <= 0 || maxCandidates > 10) {
            maxCandidates = 10;
        }
        if (maxRetries < 0) {
            maxRetries = 1;
        }
        if (staleTimeoutSeconds <= 0) {
            staleTimeoutSeconds = 600;
        }
        if (retryCooldownSeconds <= 0) {
            retryCooldownSeconds = 300;
        }
        if (maxRetryCount <= 0) {
            maxRetryCount = 3;
        }
        if (recoveryDelay == null || recoveryDelay.isNegative() || recoveryDelay.isZero()) {
            recoveryDelay = Duration.ofSeconds(5);
        }
        if (recoveryBatchSize <= 0) {
            recoveryBatchSize = 20;
        }
        if (workerConcurrency <= 0) {
            workerConcurrency = 2;
        }
    }
}
