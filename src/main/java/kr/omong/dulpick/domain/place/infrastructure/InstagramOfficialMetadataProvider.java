package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ContentMetadataProvider;
import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.config.InstagramProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

@Component
public class InstagramOfficialMetadataProvider implements ContentMetadataProvider {

    private final InstagramProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    public InstagramOfficialMetadataProvider(
            InstagramProperties properties,
            Clock clock
    ) {
        this.properties = properties;
        this.clock = clock;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean supports(ContentSourceType sourceType) {
        return properties.officialEnabled()
                && properties.accessToken() != null
                && !properties.accessToken().isBlank()
                && isInstagram(sourceType);
    }

    private boolean isInstagram(ContentSourceType sourceType) {
        return sourceType == ContentSourceType.INSTAGRAM_REEL
                || sourceType == ContentSourceType.INSTAGRAM_POST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.oembedPath())
                            .queryParam("url", canonicalUrl)
                            .queryParam("access_token", properties.accessToken())
                            .build())
                    .retrieve()
                    .body(Map.class);
            String title = text(response, "title");
            InstagramCaptionMetadataParser.Parsed parsed = InstagramCaptionMetadataParser.parse(
                    title,
                    "",
                    title
            );
            String content = String.join("\n", parsed.title(), parsed.content()).strip();
            if (content.isBlank()) {
                throw new MetadataUnavailableException();
            }
            return new ContentMetadata(
                    canonicalUrl,
                    sourceType,
                    parsed.title(),
                    parsed.content(),
                    text(response, "thumbnail_url"),
                    Sha256.hex(content),
                    clock.instant(),
                    text(response, "author_name"),
                    null,
                    null,
                    null,
                    null,
                    clock.instant()
            );
        } catch (RestClientException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String text(Map<String, Object> response, String key) {
        Object value = response == null ? null : response.get(key);
        return value == null ? "" : String.valueOf(value).strip();
    }
}
