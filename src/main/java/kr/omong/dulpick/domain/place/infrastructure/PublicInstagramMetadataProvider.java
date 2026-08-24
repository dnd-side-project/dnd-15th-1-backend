package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ContentMetadataProvider;
import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.config.InstagramProperties;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PublicInstagramMetadataProvider implements ContentMetadataProvider {

    private static final int MAX_HTML_BYTES = 2_000_000;
    private static final int MAX_REDIRECTS = 4;
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
    private static final Pattern CDN_IMAGE = Pattern.compile(
            "https?://[^\\\"'\\\\\\s<>},]+(?:cdninstagram\\.com|fbcdn\\.net)[^\\\"'\\\\\\s<>},]*",
            Pattern.CASE_INSENSITIVE
    );

    private final InstagramProperties properties;
    private final RestClient restClient;
    private final PublicWebUrlValidator urlValidator;
    private final Clock clock;

    @Autowired
    public PublicInstagramMetadataProvider(
            InstagramProperties properties,
            Clock clock,
            PublicWebUrlValidator urlValidator
    ) {
        this(properties, clock, urlValidator, createRestClientBuilder(properties));
    }

    PublicInstagramMetadataProvider(
            InstagramProperties properties,
            Clock clock,
            PublicWebUrlValidator urlValidator,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.clock = clock;
        this.urlValidator = urlValidator;
        this.restClient = restClientBuilder.build();
    }

    private static RestClient.Builder createRestClientBuilder(InstagramProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public boolean supports(ContentSourceType sourceType) {
        return properties.publicCrawlerEnabled()
                && (sourceType == ContentSourceType.INSTAGRAM_REEL
                || sourceType == ContentSourceType.INSTAGRAM_POST);
    }

    @Override
    public ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType) {
        try {
            String html = fetchFollowingRedirects(canonicalUrl);
            String title = extract(html, TITLE);
            String description = extract(html, DESCRIPTION);
            List<String> imageUrls = extractImageUrls(html).stream()
                    .limit(properties.maxImages())
                    .toList();
            String thumbnailUrl = imageUrls.isEmpty() ? "" : imageUrls.getFirst();
            InstagramCaptionMetadataParser.Parsed parsed = InstagramCaptionMetadataParser.parse(
                    title,
                    description,
                    description
            );
            if (parsed.title().isBlank() && parsed.content().isBlank()) {
                throw new MetadataUnavailableException();
            }
            String content = String.join("\n", parsed.title(), parsed.content()).strip();
            return new ContentMetadata(
                    canonicalUrl,
                    sourceType,
                    parsed.title(),
                    parsed.content(),
                    thumbnailUrl,
                    Sha256.hex(content),
                    clock.instant(),
                    parsed.displayName(),
                    parsed.username(),
                    parsed.publishedOn(),
                    parsed.likeCount(),
                    parsed.commentCount(),
                    clock.instant(),
                    imageUrls
            );
        } catch (RestClientException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    public List<String> fetchImageUrls(String canonicalUrl) {
        try {
            return extractImageUrls(fetchFollowingRedirects(canonicalUrl)).stream()
                    .limit(properties.maxImages())
                    .toList();
        } catch (RestClientException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private String fetchFollowingRedirects(String canonicalUrl) {
        String currentUrl = canonicalUrl;
        for (int redirect = 0; redirect < MAX_REDIRECTS; redirect++) {
            urlValidator.validate(currentUrl);
            FetchedResponse response = request(currentUrl);
            if (!response.status().is3xxRedirection()) {
                return response.body();
            }
            if (response.location() == null) {
                throw new MetadataUnavailableException();
            }
            currentUrl = URI.create(currentUrl).resolve(response.location()).toString();
        }
        throw new MetadataUnavailableException();
    }

    private FetchedResponse request(String url) {
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
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
        MediaType contentType = headers.getContentType();
        if (!status.is2xxSuccessful()
                || headers.getContentLength() > MAX_HTML_BYTES
                || contentType != null && !MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
            throw new MetadataUnavailableException();
        }
        byte[] body = bodyStream.readNBytes(MAX_HTML_BYTES + 1);
        if (body.length > MAX_HTML_BYTES) {
            throw new MetadataUnavailableException();
        }
        Charset charset = contentType != null && contentType.getCharset() != null
                ? contentType.getCharset()
                : StandardCharsets.UTF_8;
        return new FetchedResponse(status, null, new String(body, charset));
    }

    private String extract(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        return matcher.find()
                ? HtmlUtils.htmlUnescape(matcher.group(1).strip())
                : "";
    }

    private List<String> extractAll(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        List<String> values = new java.util.ArrayList<>();
        while (matcher.find()) {
            String value = HtmlUtils.htmlUnescape(matcher.group(1).strip());
            if (!value.isBlank() && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> extractImageUrls(String html) {
        String normalizedHtml = html
                .replace("\\u002F", "/")
                .replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("\\u003D", "=");
        List<String> values = new java.util.ArrayList<>(extractAll(normalizedHtml, IMAGE));
        Matcher matcher = CDN_IMAGE.matcher(HtmlUtils.htmlUnescape(normalizedHtml));
        while (matcher.find()) {
            String value = matcher.group().strip();
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private record FetchedResponse(HttpStatusCode status, URI location, String body) {
    }
}
