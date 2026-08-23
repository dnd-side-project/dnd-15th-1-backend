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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicWebMetadataProvider implements ContentMetadataProvider {

    private static final int MAX_HTML_BYTES = 1_000_000;
    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";
    private static final Set<ContentSourceType> SUPPORTED_TYPES = Set.of(
            ContentSourceType.NAVER_MAP,
            ContentSourceType.NAVER_BLOG,
            ContentSourceType.NAVER_SHORT_LINK,
            ContentSourceType.KAKAO_MAP,
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
    private static final Pattern KAKAO_PLACE_PATH = Pattern.compile(
            "^/(\\d+)/?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern KAKAO_LINK_PLACE_PATH = Pattern.compile(
            "^/link/(?:map|to)/(\\d+)/?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern KAKAO_PLACE_QUERY = Pattern.compile(
            "(?:^|&)id=(\\d+)(?:&|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BLOG_FRAME = Pattern.compile(
            "<iframe[^>]+id=[\\\"']mainFrame[\\\"'][^>]+src=[\\\"']([^\\\"']+)",
            Pattern.CASE_INSENSITIVE
    );

    private final PlaceAnalysisProperties properties;
    private final RestClient restClient;
    private final NaverPlaceHtmlParser naverPlaceHtmlParser;
    private final PublicWebUrlValidator urlValidator;
    private final Clock clock;

    @Autowired
    public PublicWebMetadataProvider(
            PlaceAnalysisProperties properties,
            Clock clock,
            NaverPlaceHtmlParser naverPlaceHtmlParser,
            PublicWebUrlValidator urlValidator
    ) {
        this(properties, clock, createRestClientBuilder(), naverPlaceHtmlParser, urlValidator);
    }

    PublicWebMetadataProvider(
            PlaceAnalysisProperties properties,
            Clock clock,
            RestClient.Builder restClientBuilder,
            NaverPlaceHtmlParser naverPlaceHtmlParser,
            PublicWebUrlValidator urlValidator
    ) {
        this.properties = properties;
        this.clock = clock;
        this.restClient = restClientBuilder.build();
        this.naverPlaceHtmlParser = naverPlaceHtmlParser;
        this.urlValidator = urlValidator;
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
            if (sourceType == ContentSourceType.KAKAO_MAP) {
                return fetchKakaoPlaceMetadata(canonicalUrl, sourceType);
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
        String frameUrl = resolve(
                page.url(),
                URI.create(HtmlUtils.htmlUnescape(frame.group(1)))
        );
        return fetchFollowingRedirects(frameUrl).body();
    }

    private FetchedPage fetchFollowingRedirects(
            String canonicalUrl
    ) {
        String currentUrl = canonicalUrl;
        for (int redirect = 0; redirect < 4; redirect++) {
            urlValidator.validate(currentUrl);
            FetchedResponse response = request(currentUrl);
            if (response.status().is3xxRedirection()) {
                URI location = response.location();
                if (location == null) {
                    throw new MetadataUnavailableException();
                }
                currentUrl = resolve(currentUrl, location);
                continue;
            }
            return new FetchedPage(currentUrl, response.body());
        }
        throw new MetadataUnavailableException();
    }

    private FetchedResponse request(String url) {
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9")
                .accept(MediaType.TEXT_HTML)
                .exchange((request, response) -> readResponse(
                        response.getStatusCode(),
                        response.getHeaders(),
                        response.getBody()
                ));
    }

    private FetchedResponse readResponse(
            HttpStatusCode status,
            HttpHeaders headers,
            InputStream bodyStream
    ) throws IOException {
        if (status.is3xxRedirection()) {
            return new FetchedResponse(status, headers.getLocation(), null);
        }
        if (!status.is2xxSuccessful()) {
            throw new MetadataUnavailableException();
        }
        MediaType contentType = headers.getContentType();
        validateHtmlHeaders(contentType, headers.getContentLength());
        byte[] body = readLimited(bodyStream);
        Charset charset = contentType != null && contentType.getCharset() != null
                ? contentType.getCharset()
                : StandardCharsets.UTF_8;
        return new FetchedResponse(status, null, new String(body, charset));
    }

    private void validateHtmlHeaders(MediaType contentType, long contentLength) {
        if (contentLength > MAX_HTML_BYTES
                || contentType != null && !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
            throw new MetadataUnavailableException();
        }
    }

    private byte[] readLimited(InputStream bodyStream) throws IOException {
        byte[] body = bodyStream.readNBytes(MAX_HTML_BYTES + 1);
        if (body.length > MAX_HTML_BYTES) {
            throw new MetadataUnavailableException();
        }
        return body;
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

    private ContentMetadata fetchKakaoPlaceMetadata(
            String canonicalUrl,
            ContentSourceType sourceType
    ) {
        FetchedPage page = fetchFollowingRedirects(canonicalUrl);
        String placeId = resolveKakaoPlaceId(page.url());
        FetchedPage placePage = isKakaoPlacePage(page.url())
                ? page
                : fetchFollowingRedirects("https://place.map.kakao.com/" + placeId);
        String title = firstNonBlank(extract(placePage.body(), TITLE), extract(placePage.body(), HTML_TITLE));
        String description = firstNonBlank(
                extract(placePage.body(), DESCRIPTION),
                extract(placePage.body(), META_DESCRIPTION)
        );
        if (title.isBlank() || description.isBlank()) {
            throw new MetadataUnavailableException();
        }
        String content = String.join("\n", title, description).strip();
        return new ContentMetadata(
                canonicalUrl,
                sourceType,
                cleanKakaoTitle(title),
                description,
                extract(placePage.body(), IMAGE),
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

    private String resolveKakaoPlaceId(String url) {
        try {
            URI uri = new URI(url);
            Matcher pathMatcher = KAKAO_PLACE_PATH.matcher(uri.getPath());
            if (pathMatcher.matches()) {
                return pathMatcher.group(1);
            }
            Matcher linkMatcher = KAKAO_LINK_PLACE_PATH.matcher(uri.getPath());
            if (linkMatcher.matches()) {
                return linkMatcher.group(1);
            }
            Matcher queryMatcher = uri.getQuery() == null
                    ? null
                    : KAKAO_PLACE_QUERY.matcher(uri.getQuery());
            if (queryMatcher != null && queryMatcher.find()) {
                return queryMatcher.group(1);
            }
            throw new MetadataUnavailableException();
        } catch (URISyntaxException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private boolean isKakaoPlacePage(String url) {
        try {
            URI uri = new URI(url);
            return "place.map.kakao.com".equalsIgnoreCase(uri.getHost())
                    && KAKAO_PLACE_PATH.matcher(uri.getPath()).matches();
        } catch (URISyntaxException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String cleanKakaoTitle(String title) {
        return title.replaceFirst("\\s*[|:]\\s*카카오맵.*$", "").strip();
    }

    private String followRedirect(String currentUrl) {
        urlValidator.validate(currentUrl);
        var response = request(currentUrl);
        if (!response.status().is3xxRedirection()) {
            throw new MetadataUnavailableException();
        }
        URI location = response.location();
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
            urlValidator.validate(resolved.toString());
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

    private record FetchedResponse(
            HttpStatusCode status,
            URI location,
            String body
    ) {
    }

}
