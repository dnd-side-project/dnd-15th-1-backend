package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kakao.local")
public record KakaoProperties(
        boolean enabled,
        String restApiKey,
        String baseUrl,
        int timeoutSeconds
) {

    public KakaoProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://dapi.kakao.com";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 3;
        }
    }
}
