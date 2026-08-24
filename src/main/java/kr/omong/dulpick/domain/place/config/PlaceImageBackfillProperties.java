package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("place.image-backfill")
public record PlaceImageBackfillProperties(
        boolean enabled,
        int maxPlaces,
        long delayMillis
) {

    public PlaceImageBackfillProperties {
        if (maxPlaces <= 0) {
            maxPlaces = 10_000;
        }
        if (delayMillis < 0) {
            delayMillis = 500;
        }
    }
}
