package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("instagram.metadata")
public record InstagramProperties(
        boolean officialEnabled,
        String accessToken,
        String baseUrl,
        String oembedPath,
        boolean publicCrawlerEnabled,
        int timeoutSeconds
) {

    public InstagramProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://graph.facebook.com";
        }
        if (oembedPath == null || oembedPath.isBlank()) {
            oembedPath = "/v22.0/instagram_oembed";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 5;
        }
    }
}
