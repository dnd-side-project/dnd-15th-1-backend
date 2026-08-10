package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ContentMetadataProvider;
import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicWebMetadataProvider implements ContentMetadataProvider {

    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";
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
    private static final Pattern BLOG_FRAME = Pattern.compile(
            "<iframe[^>]+id=[\\\"']mainFrame[\\\"'][^>]+src=[\\\"']([^\\\"']+)",
            Pattern.CASE_INSENSITIVE
    );

    private final PlaceAnalysisProperties properties;
    private final RestClient restClient;
    private final NaverPlaceHtmlParser naverPlaceHtmlParser;
    private final Clock clock;

    @Autowired
    public PublicWebMetadataProvider(PlaceAnalysisProperties properties, Clock clock) {
        this(properties, clock, createRestClientBuilder(), new NaverPlaceHtmlParser());
    }

    PublicWebMetadataProvider(
            PlaceAnalysisProperties properties,
            Clock clock,
            RestClient.Builder restClientBuilder,
            NaverPlaceHtmlParser naverPlaceHtmlParser
    ) {
        this.properties = properties;
        this.clock = clock;
        this.restClient = configure(restClientBuilder).build();
        this.naverPlaceHtmlParser = naverPlaceHtmlParser;
    }

    private static RestClient.Builder createRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory);
    }

    private static RestClient.Builder configure(RestClient.Builder builder) {
        return builder.configureMessageConverters(converters -> converters
                .registerDefaults()
                .withStringConverter(new StringHttpMessageConverter(StandardCharsets.UTF_8))
        );
    }

    @Override
    public boolean supports(ContentSourceType sourceType) {
        return properties.publicCrawlerEnabled() && SUPPORTED_TYPES.contains(sourceType);
    }

    @Override
    public ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType) {
        try {
            if (isNaverPlace(sourceType)) {
                return fetchNaverPlaceMetadata(canonicalUrl, sourceType);
            }
            FetchedPage page = fetchFollowingRedirects(canonicalUrl);
            String html = page.body();
            if (sourceType == ContentSourceType.NAVER_BLOG) {
                html = fetchBlogFrame(page, html);
            }
            String title = firstNonBlank(extract(html, TITLE), extract(html, HTML_TITLE));
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
            validateAllowedUrl(currentUrl);
            ResponseEntity<String> response = request(currentUrl);
            if (response.getStatusCode().is3xxRedirection()) {
                URI location = response.getHeaders().getLocation();
                if (location == null) {
                    throw new MetadataUnavailableException();
                }
                currentUrl = resolve(currentUrl, location);
                continue;
            }
            validateHtmlResponse(response.getHeaders().getContentType(), response.getBody());
            return new FetchedPage(currentUrl, response.getBody());
        }
        throw new MetadataUnavailableException();
    }

    private ResponseEntity<String> request(String url) {
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9")
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .onStatus(HttpStatusCode::is3xxRedirection, (request, response) -> {
                })
                .toEntity(String.class);
    }

    private ContentMetadata fetchNaverPlaceMetadata(
            String canonicalUrl,
            ContentSourceType sourceType
    ) {
        String placeId = resolveNaverPlaceId(canonicalUrl);
        NaverPlaceHtmlParser.ParsedPlace details = fetchNaverPlaceDetails(placeId);
        String title = details.name();
        String caption = details.address();
        String content = (title + "\n" + caption).strip();
        if (content.isBlank()) {
            throw new MetadataUnavailableException();
        }
        return new ContentMetadata(
                canonicalUrl,
                sourceType,
                title,
                caption,
                null,
                Sha256.hex(content),
                clock.instant(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String resolveNaverPlaceId(String canonicalUrl) {
        String currentUrl = canonicalUrl;
        for (int redirect = 0; redirect < 4; redirect++) {
            Matcher matcher = NAVER_PLACE_ID.matcher(currentUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
            currentUrl = followRedirect(currentUrl);
        }
        throw new MetadataUnavailableException();
    }

    private String followRedirect(String currentUrl) {
        validateAllowedUrl(currentUrl);
        var response = request(currentUrl);
        if (!response.getStatusCode().is3xxRedirection()) {
            throw new MetadataUnavailableException();
        }
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new MetadataUnavailableException();
        }
        return resolve(currentUrl, location);
    }

    private NaverPlaceHtmlParser.ParsedPlace fetchNaverPlaceDetails(String placeId) {
        FetchedPage mobilePage = fetchFollowingRedirects(
                "https://m.place.naver.com/place/" + placeId + "/home"
        );
        return naverPlaceHtmlParser.parse(mobilePage.body());
    }

    private boolean isNaverPlace(ContentSourceType sourceType) {
        return sourceType == ContentSourceType.NAVER_SHORT_LINK
                || sourceType == ContentSourceType.NAVER_MAP;
    }

    private String resolve(String currentUrl, URI location) {
        try {
            URI resolved = new URI(currentUrl).resolve(location);
            if (!"https".equalsIgnoreCase(resolved.getScheme())) {
                throw new MetadataUnavailableException();
            }
            validateAllowedUrl(resolved.toString());
            return resolved.toString();
        } catch (URISyntaxException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private void validateAllowedUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            boolean allowedHost = "naver.me".equalsIgnoreCase(host)
                    || isHostOrSubdomain(host, "naver.com")
                    || isHostOrSubdomain(host, "tistory.com");
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null
                    || uri.getPort() != -1
                    || !allowedHost) {
                throw new MetadataUnavailableException();
            }
        } catch (URISyntaxException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private boolean isHostOrSubdomain(String host, String domain) {
        return domain.equalsIgnoreCase(host)
                || host != null && host.toLowerCase().endsWith("." + domain);
    }

    private void validateHtmlResponse(MediaType contentType, String body) {
        if (body == null
                || body.length() > 1_000_000
                || contentType != null && !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
            throw new MetadataUnavailableException();
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
