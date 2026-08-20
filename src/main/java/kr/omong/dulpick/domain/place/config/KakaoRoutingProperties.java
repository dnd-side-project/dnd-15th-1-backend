package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kakao.routing")
public record KakaoRoutingProperties(
        boolean enabled,
        String restApiKey,
        String baseUrl,
        int timeoutSeconds
) {

    public KakaoRoutingProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://dapi.kakao.com";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 3;
        }
    }
}
