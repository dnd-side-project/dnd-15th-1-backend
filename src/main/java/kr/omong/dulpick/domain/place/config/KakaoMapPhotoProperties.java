package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kakao.map-photo-scraping")
public record KakaoMapPhotoProperties(
        boolean enabled,
        String baseUrl,
        int timeoutSeconds,
        int maxImages,
        String appVersion
) {

    public KakaoMapPhotoProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://place-api.map.kakao.com";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 2;
        }
        if (maxImages <= 0) {
            maxImages = 5;
        }
        if (appVersion == null || appVersion.isBlank()) {
            appVersion = "6.6.0";
        }
    }
}
