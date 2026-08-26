package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.PlaceImageProvider;
import kr.omong.dulpick.domain.place.config.KakaoMapPhotoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KakaoMapPhotoProvider implements PlaceImageProvider {

    private static final Logger logger = LoggerFactory.getLogger(KakaoMapPhotoProvider.class);
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; DulPick/1.0)";
    private static final int MAX_URL_LENGTH = 2_000;
    private static final Set<String> EXPLICITLY_ALLOWED_IMAGE_HOSTS = Set.of(
            "kakaocdn.net",
            "map.kakaocdn.net",
            "daumcdn.net",
            "postfiles.pstatic.net",
            "dthumb-phinf.pstatic.net"
    );

    private final KakaoMapPhotoProperties properties;
    private final RestClient restClient;

    @Autowired
    public KakaoMapPhotoProvider(KakaoMapPhotoProperties properties) {
        this(properties, createRestClientBuilder(properties));
    }

    KakaoMapPhotoProvider(
            KakaoMapPhotoProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    private static RestClient.Builder createRestClientBuilder(
            KakaoMapPhotoProperties properties
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findImageUrls(String externalPlaceId) {
        if (!properties.enabled() || !isValidPlaceId(externalPlaceId)) {
            return List.of();
        }
        for (int attempt = 1; attempt <= properties.retryAttempts(); attempt++) {
            List<String> imageUrls = requestImageUrls(externalPlaceId);
            if (!imageUrls.isEmpty() || attempt == properties.retryAttempts()) {
                return imageUrls;
            }
            if (!waitBeforeRetry()) {
                return List.of();
            }
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> requestImageUrls(String externalPlaceId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/places/tab/photos/{placeId}")
                            .queryParam("page", 1)
                            .build(externalPlaceId))
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .header(HttpHeaders.REFERER, "https://place.map.kakao.com/" + externalPlaceId)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header("appVersion", properties.appVersion())
                    .header("pf", "PC")
                    .retrieve()
                    .body(Map.class);
            Object photos = response == null ? null : response.get("photos");
            if (!(photos instanceof List<?> values)) {
                logger.warn("kakao_map_photo_empty_response placeId={} reason=INVALID_PAYLOAD", externalPlaceId);
                return List.of();
            }
            LinkedHashSet<String> imageUrls = new LinkedHashSet<>();
            int inspectedCount = 0;
            int rejectedCount = 0;
            for (Object value : values) {
                inspectedCount++;
                if (!(value instanceof Map<?, ?> photo)) {
                    rejectedCount++;
                    continue;
                }
                String imageUrl = normalizeImageUrl(photo.get("url"));
                if (imageUrl != null) {
                    imageUrls.add(imageUrl);
                } else {
                    rejectedCount++;
                }
                if (imageUrls.size() >= properties.maxImages()) {
                    break;
                }
            }
            if (imageUrls.isEmpty() && !values.isEmpty()) {
                logger.warn(
                        "kakao_map_photo_empty_response placeId={} reason=ALL_URLS_REJECTED photoCount={} rejectedCount={}",
                        externalPlaceId,
                        inspectedCount,
                        rejectedCount
                );
            }
            return List.copyOf(imageUrls);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Kakao Map photo scraping failed: placeId={}, cause={}",
                    externalPlaceId,
                    exception.getClass().getSimpleName()
            );
            return List.of();
        }
    }

    private boolean waitBeforeRetry() {
        if (properties.retryDelayMillis() == 0) {
            return true;
        }
        try {
            Thread.sleep(properties.retryDelayMillis());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isValidPlaceId(String placeId) {
        return placeId != null && placeId.matches("\\d{1,80}");
    }

    private String normalizeImageUrl(Object rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String value = rawUrl.toString().strip();
        if (value.startsWith("//")) {
            value = "https:" + value;
        } else if (value.startsWith("http://")) {
            value = "https://" + value.substring("http://".length());
        }
        if (value.length() > MAX_URL_LENGTH) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !isAllowedImageHost(uri.getHost())) {
                return null;
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isAllowedImageHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(java.util.Locale.ROOT);
        return EXPLICITLY_ALLOWED_IMAGE_HOSTS.contains(normalized)
                || normalized.endsWith(".kakaocdn.net")
                || normalized.endsWith(".daumcdn.net")
                || normalized.equals("postfiles.pstatic.net")
                || normalized.equals("dthumb-phinf.pstatic.net");
    }
}
