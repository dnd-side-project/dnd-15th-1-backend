package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ContentMetadataProvider;
import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicWebMetadataProvider implements ContentMetadataProvider {

    private static final Set<ContentSourceType> SUPPORTED_TYPES = Set.of(
            ContentSourceType.NAVER_MAP,
            ContentSourceType.NAVER_BLOG,
            ContentSourceType.NAVER_SHORT_LINK,
            ContentSourceType.TISTORY
    );
    private static final Pattern TITLE = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:title[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DESCRIPTION = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:description[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern META_DESCRIPTION = Pattern.compile(
            "<meta[^>]+name=[\\\"']description[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IMAGE = Pattern.compile(
            "<meta[^>]+property=[\\\"']og:image[\\\"'][^>]+content=[\\\"']([^\\\"']*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HTML_TITLE = Pattern.compile(
            "<title[^>]*>(.*?)</title>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern NAVER_PLACE_ID = Pattern.compile(
            "/entry/place/(\\d+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JSON_NAME = Pattern.compile(
            "\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BLOG_FRAME = Pattern.compile(
            "<iframe[^>]+id=[\\\"']mainFrame[\\\"'][^>]+src=[\\\"']([^\\\"']+)",
            Pattern.CASE_INSENSITIVE
    );

    private final PlaceAnalysisProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    public PublicWebMetadataProvider(PlaceAnalysisProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean supports(ContentSourceType sourceType) {
        return properties.publicCrawlerEnabled() && SUPPORTED_TYPES.contains(sourceType);
    }

    @Override
    public ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType) {
        try {
            FetchedPage page = fetchFollowingRedirects(canonicalUrl);
            String html = page.body();
            if (html == null
                    || html.length() > 1_000_000) {
                throw new MetadataUnavailableException();
            }
            if (sourceType == ContentSourceType.NAVER_BLOG) {
                html = fetchBlogFrame(page, html);
            }
            String title = firstNonBlank(extract(html, TITLE), extract(html, HTML_TITLE));
            if (title.isBlank() && sourceType == ContentSourceType.NAVER_SHORT_LINK) {
                title = fetchNaverPlaceTitle(page.url());
            }
            String description = firstNonBlank(
                    extract(html, DESCRIPTION),
                    extract(html, META_DESCRIPTION)
            );
            if (description.isBlank()) {
                description = extractBodyText(html);
            }
            String content = String.join("\n", title, description).strip();
            if (content.isBlank()) {
                throw new MetadataUnavailableException();
            }
            return new ContentMetadata(
                    canonicalUrl,
                    sourceType,
                    title,
                    description,
                    extract(html, IMAGE),
                    Sha256.hex(content),
                    clock.instant(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } catch (RestClientException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String fetchBlogFrame(FetchedPage page, String html) {
        Matcher frame = BLOG_FRAME.matcher(html);
        if (!frame.find()) {
            return html;
        }
        String frameUrl = resolve(page.url(), URI.create(frame.group(1)));
        return fetchFollowingRedirects(frameUrl).body();
    }

    private FetchedPage fetchFollowingRedirects(
            String canonicalUrl
    ) {
        String currentUrl = canonicalUrl;
        for (int redirect = 0; redirect < 4; redirect++) {
            var response = restClient.get()
                    .uri(currentUrl)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .onStatus(HttpStatusCode::is3xxRedirection, (request, redirectResponse) -> {
                    })
                    .toEntity(String.class);
            if (response.getHeaders().getLocation() == null) {
                return new FetchedPage(currentUrl, response.getBody());
            }
            currentUrl = resolve(currentUrl, response.getHeaders().getLocation());
        }
        throw new MetadataUnavailableException();
    }

    private String fetchNaverPlaceTitle(String finalUrl) {
        Matcher matcher = NAVER_PLACE_ID.matcher(finalUrl);
        if (!matcher.find()) {
            throw new MetadataUnavailableException();
        }
        try {
            String body = restClient.get()
                    .uri("https://map.naver.com/p/api/place/" + matcher.group(1))
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                    .header(HttpHeaders.REFERER, finalUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            Matcher name = JSON_NAME.matcher(body == null ? "" : body);
            if (!name.find()) {
                throw new MetadataUnavailableException();
            }
            return HtmlUtils.htmlUnescape(name.group(1).strip());
        } catch (RestClientException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String resolve(String currentUrl, URI location) {
        try {
            URI resolved = new URI(currentUrl).resolve(location);
            if (!"https".equalsIgnoreCase(resolved.getScheme())) {
                throw new MetadataUnavailableException();
            }
            return resolved.toString();
        } catch (URISyntaxException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String extract(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? HtmlUtils.htmlUnescape(matcher.group(1).strip()) : "";
    }

    private String firstNonBlank(String first, String second) {
        return first.isBlank() ? second : first;
    }

    private String extractBodyText(String html) {
        String withoutScripts = html
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ");
        String normalized = HtmlUtils.htmlUnescape(withoutScripts)
                .replaceAll("\\s+", " ")
                .strip();
        return normalized.substring(0, Math.min(normalized.length(), 20_000));
    }

    private record FetchedPage(String url, String body) {
    }
}
