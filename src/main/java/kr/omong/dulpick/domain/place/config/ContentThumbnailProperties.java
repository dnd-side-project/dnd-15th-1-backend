package kr.omong.dulpick.domain.place.config;

import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("content.thumbnail")
public record ContentThumbnailProperties(
        String baseUrl,
        String storagePath,
        int timeoutSeconds,
        long maxBytes,
        int maxImages
) {

    @ConstructorBinding
    public ContentThumbnailProperties {
        baseUrl = normalizeBaseUrl(baseUrl);
        if (storagePath == null || storagePath.isBlank()) {
            storagePath = "./data/content-images";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 10;
        }
        if (maxBytes <= 0) {
            maxBytes = 5_000_000L;
        }
        if (maxImages <= 0) {
            maxImages = 10;
        }
    }

    public ContentThumbnailProperties(
            String baseUrl,
            String storagePath,
            int timeoutSeconds,
            long maxBytes
    ) {
        this(baseUrl, storagePath, timeoutSeconds, maxBytes, 10);
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.strip().replaceAll("/+$", "");
    }
}
