package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentThumbnailDownloader;
import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
public final class KakaoMapImageDownloader implements ContentThumbnailDownloader {

    private static final String KAKAO_REFERER = "https://place.map.kakao.com/";
    private final ContentThumbnailProperties properties;
    private final HostAddressResolver addressResolver;
    private final RestClient restClient;

    @Autowired
    public KakaoMapImageDownloader(
            ContentThumbnailProperties properties,
            HostAddressResolver addressResolver
    ) {
        this(properties, addressResolver, createRestClientBuilder(properties));
    }

    KakaoMapImageDownloader(
            ContentThumbnailProperties properties,
            HostAddressResolver addressResolver,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.addressResolver = addressResolver;
        this.restClient = restClientBuilder.build();
    }

    private static RestClient.Builder createRestClientBuilder(ContentThumbnailProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public ContentThumbnailDownloader.DownloadedThumbnail download(String sourceUrl) {
        URI uri = validate(sourceUrl);
        try {
            return restClient.get()
                    .uri(uri)
                    .headers(headers -> {
                        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; DulPick/1.0)");
                        String referer = refererFor(uri);
                        if (referer != null) {
                            headers.set(HttpHeaders.REFERER, referer);
                        }
                        headers.set(HttpHeaders.ACCEPT, "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
                    })
                    .exchange((request, response) -> readResponse(
                            response.getStatusCode(), response.getHeaders(), response.getBody()
                    ));
        } catch (RestClientException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
    }

    private String refererFor(URI uri) {
        return isNaverImageHost(uri.getHost()) ? null : KAKAO_REFERER;
    }

    private URI validate(String sourceUrl) {
        URI uri;
        try {
            uri = new URI(sourceUrl);
        } catch (URISyntaxException exception) {
            throw new PublicContentImageUnavailableException(exception);
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || !isAllowedHost(host)) {
            throw new PublicContentImageUnavailableException();
        }
        List<InetAddress> addresses = addressResolver.resolve(host);
        if (addresses.isEmpty() || addresses.stream().anyMatch(this::isNonPublic)) {
            throw new PublicContentImageUnavailableException();
        }
        return uri;
    }

    private boolean isAllowedHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("kakaocdn.net")
                || normalized.endsWith(".kakaocdn.net")
                || normalized.equals("daumcdn.net")
                || normalized.endsWith(".daumcdn.net")
                || normalized.equals("postfiles.pstatic.net")
                || normalized.equals("dthumb-phinf.pstatic.net");
    }

    private boolean isNaverImageHost(String host) {
        return "postfiles.pstatic.net".equalsIgnoreCase(host)
                || "dthumb-phinf.pstatic.net".equalsIgnoreCase(host);
    }

    private boolean isNonPublic(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isIpv6UniqueLocal(address);
    }

    private boolean isIpv6UniqueLocal(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        int firstByte = address.getAddress()[0] & 0xFF;
        return (firstByte & 0xFE) == 0xFC;
    }

    private ContentThumbnailDownloader.DownloadedThumbnail readResponse(
            HttpStatusCode status,
            HttpHeaders headers,
            InputStream bodyStream
    ) throws IOException {
        MediaType contentType = headers.getContentType();
        long contentLength = headers.getContentLength();
        if (!status.is2xxSuccessful()
                || contentType == null
                || !"image".equalsIgnoreCase(contentType.getType())
                || contentLength > properties.maxBytes()) {
            throw new PublicContentImageUnavailableException();
        }
        byte[] bytes = bodyStream.readNBytes(Math.toIntExact(properties.maxBytes()) + 1);
        if (bytes.length == 0 || bytes.length > properties.maxBytes()) {
            throw new PublicContentImageUnavailableException();
        }
        return new ContentThumbnailDownloader.DownloadedThumbnail(bytes, contentType);
    }
}
