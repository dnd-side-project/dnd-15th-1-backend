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
        PlaceImageEnrichmentProperties.class,
        InstagramProperties.class
})
public class PlacePropertiesConfig {
}
