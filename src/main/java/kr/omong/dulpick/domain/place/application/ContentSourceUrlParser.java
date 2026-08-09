package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.InvalidSourceUrlException;
import kr.omong.dulpick.domain.place.application.exception.UnsupportedSourceUrlException;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class ContentSourceUrlParser {

    public ParsedSource parse(String rawUrl) {
        URI uri = parseUri(rawUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new InvalidSourceUrlException();
        }
        ContentSourceType sourceType = sourceType(uri);
        if (sourceType == null) {
            throw new UnsupportedSourceUrlException();
        }
        return new ParsedSource(canonicalize(uri), sourceType);
    }

    private URI parseUri(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.length() > 2_000) {
            throw new InvalidSourceUrlException();
        }
        try {
            return new URI(rawUrl.strip());
        } catch (URISyntaxException exception) {
            throw new InvalidSourceUrlException();
        }
    }

    private ContentSourceType sourceType(URI uri) {
        String host = uri.getHost();
        String path = uri.getPath();
        if (isInstagramHost(host)) {
            return instagramSourceType(path);
        }
        if ("naver.me".equalsIgnoreCase(host)) {
            return ContentSourceType.NAVER_SHORT_LINK;
        }
        if (isHostOrSubdomain(host, "map.naver.com")) {
            return ContentSourceType.NAVER_MAP;
        }
        if (isHostOrSubdomain(host, "blog.naver.com")) {
            return ContentSourceType.NAVER_BLOG;
        }
        if (isHostOrSubdomain(host, "tistory.com")) {
            return ContentSourceType.TISTORY;
        }
        return null;
    }

    private ContentSourceType instagramSourceType(String path) {
        if (path != null && path.matches("^/reel/[^/]+/?$")) {
            return ContentSourceType.INSTAGRAM_REEL;
        }
        if (path != null && path.matches("^/(p|posts)/[^/]+/?$")) {
            return ContentSourceType.INSTAGRAM_POST;
        }
        throw new UnsupportedSourceUrlException();
    }

    private boolean isInstagramHost(String host) {
        return "instagram.com".equalsIgnoreCase(host)
                || "www.instagram.com".equalsIgnoreCase(host);
    }

    private boolean isHostOrSubdomain(String host, String domain) {
        return domain.equalsIgnoreCase(host)
                || (host != null && host.toLowerCase().endsWith("." + domain));
    }

    private String canonicalize(URI uri) {
        try {
            return new URI(
                    "https",
                    null,
                    uri.getHost().toLowerCase(),
                    -1,
                    uri.getPath().replaceAll("/+$", ""),
                    null,
                    null
            ).toString();
        } catch (URISyntaxException exception) {
            throw new InvalidSourceUrlException();
        }
    }

    public record ParsedSource(
            String canonicalUrl,
            ContentSourceType sourceType
    ) {
    }
}
