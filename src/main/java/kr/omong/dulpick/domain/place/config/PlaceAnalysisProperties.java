package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("place-analysis")
public record PlaceAnalysisProperties(
        boolean enabled,
        int dailyLimit,
        int maxCandidates,
        int maxRetries,
        boolean publicCrawlerEnabled,
        int staleTimeoutSeconds
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
    }
}
