package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("content.image-backfill")
public record ContentImageBackfillProperties(
        boolean enabled,
        int maxContents,
        long delayMillis
) {

    public ContentImageBackfillProperties {
        if (maxContents <= 0) {
            maxContents = 10_000;
        }
        if (delayMillis < 0) {
            delayMillis = 500;
        }
    }
}
