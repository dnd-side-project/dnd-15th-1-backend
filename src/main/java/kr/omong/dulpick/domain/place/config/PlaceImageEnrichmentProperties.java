package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("place-analysis.image-enrichment")
public record PlaceImageEnrichmentProperties(
        Duration retryCooldown,
        int maxAttempts
) {

    public PlaceImageEnrichmentProperties {
        if (retryCooldown == null || retryCooldown.isNegative()) {
            retryCooldown = Duration.ofMinutes(10);
        }
        if (maxAttempts <= 0 || maxAttempts > 10) {
            maxAttempts = 3;
        }
    }
}
