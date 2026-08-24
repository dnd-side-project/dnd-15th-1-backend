package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PlaceAnalysisProperties.class,
        GeminiProperties.class,
        KakaoProperties.class,
        KakaoRoutingProperties.class,
        KakaoMapPhotoProperties.class,
        InstagramProperties.class,
        ContentThumbnailProperties.class,
        ContentImageBackfillProperties.class,
        PlaceImageEnrichmentProperties.class
})
public class PlacePropertiesConfig {
}
