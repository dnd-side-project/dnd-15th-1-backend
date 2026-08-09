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
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicInstagramMetadataProvider implements ContentMetadataProvider {

    private static final Pattern TITLE = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:title[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DESCRIPTION = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:description[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IMAGE = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:image[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );

    private final InstagramProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    public PublicInstagramMetadataProvider(
            InstagramProperties properties,
            Clock clock
    ) {
        this.properties = properties;
        this.clock = clock;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean supports(ContentSourceType sourceType) {
        return properties.publicCrawlerEnabled();
    }

    @Override
    public ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType) {
        try {
            var response = restClient.get()
                    .uri(canonicalUrl)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .toEntity(String.class);
            if (response.getHeaders().getLocation() != null
                    || response.getBody() == null
                    || response.getBody().length() > 1_000_000) {
                throw new MetadataUnavailableException();
            }
            String html = response.getBody();
            String title = extract(html, TITLE);
            String description = extract(html, DESCRIPTION);
            String thumbnailUrl = extract(html, IMAGE);
            String content = String.join("\n", title, description).strip();
            if (content.isBlank()) {
                throw new MetadataUnavailableException();
            }
            return new ContentMetadata(
                    canonicalUrl,
                    sourceType,
                    title,
                    description,
                    thumbnailUrl,
                    Sha256.hex(content),
                    clock.instant()
            );
        } catch (RestClientException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String extract(String html, Pattern pattern) {
        if (html == null) {
            return "";
        }
        Matcher matcher = pattern.matcher(html);
        return matcher.find()
                ? HtmlUtils.htmlUnescape(matcher.group(1).strip())
                : "";
    }
}
