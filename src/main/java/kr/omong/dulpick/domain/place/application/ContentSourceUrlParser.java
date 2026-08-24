package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.InvalidSourceUrlException;
import kr.omong.dulpick.domain.place.application.exception.UnsupportedSourceUrlException;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Component
public class ContentSourceUrlParser {

    private static final Pattern KAKAO_PLACE_PATH = Pattern.compile("^/(\\d+)/?$");
    private static final Pattern KAKAO_LINK_PLACE_PATH = Pattern.compile("^/link/(?:map|to)/(\\d+)/?$");
    private static final Pattern KAKAO_APP_PLACE_PATH = Pattern.compile("^/(?:place|scheme/place)/?$");
    private static final Pattern PLACE_ID_QUERY = Pattern.compile("(?:^|&)id=(\\d+)(?:&|$)");

    public ParsedSource parse(String rawUrl) {
        URI uri = parseUri(rawUrl);
        if (!isAllowedScheme(uri)) {
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
        if (isKakaoPlaceUrl(uri)) {
            return ContentSourceType.KAKAO_MAP;
        }
        if (isHostOrSubdomain(host, "tistory.com")) {
            return ContentSourceType.TISTORY;
        }
        return null;
    }

    private boolean isAllowedScheme(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme())
                || ("http".equalsIgnoreCase(uri.getScheme())
                && "kko.to".equalsIgnoreCase(uri.getHost()));
    }

    private boolean isKakaoPlaceUrl(URI uri) {
        String host = uri.getHost();
        if ("kko.to".equalsIgnoreCase(host)) {
            return uri.getPath() != null && !uri.getPath().equals("/");
        }
        if ("place.map.kakao.com".equalsIgnoreCase(host)) {
            return KAKAO_PLACE_PATH.matcher(uri.getPath()).matches();
        }
        if ("map.kakao.com".equalsIgnoreCase(host)) {
            return KAKAO_LINK_PLACE_PATH.matcher(uri.getPath()).matches();
        }
        if ("applink.map.kakao.com".equalsIgnoreCase(host)
                || "m.map.kakao.com".equalsIgnoreCase(host)) {
            return KAKAO_APP_PLACE_PATH.matcher(uri.getPath()).matches()
                    && hasPlaceIdQuery(uri.getQuery());
        }
        return false;
    }

    private boolean hasPlaceIdQuery(String query) {
        return query != null && PLACE_ID_QUERY.matcher(query).find();
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
            String kakaoPlaceId = kakaoPlaceId(uri);
            if (kakaoPlaceId != null) {
                return new URI(
                        "https",
                        null,
                        "place.map.kakao.com",
                        -1,
                        "/" + kakaoPlaceId,
                        null,
                        null
                ).toString();
            }
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

    private String kakaoPlaceId(URI uri) {
        String host = uri.getHost();
        Pattern pathPattern = null;
        if ("place.map.kakao.com".equalsIgnoreCase(host)) {
            pathPattern = KAKAO_PLACE_PATH;
        } else if ("map.kakao.com".equalsIgnoreCase(host)) {
            pathPattern = KAKAO_LINK_PLACE_PATH;
        }
        if (pathPattern != null) {
            var matcher = pathPattern.matcher(uri.getPath());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        if ("applink.map.kakao.com".equalsIgnoreCase(host)
                || "m.map.kakao.com".equalsIgnoreCase(host)) {
            var matcher = PLACE_ID_QUERY.matcher(uri.getQuery() == null ? "" : uri.getQuery());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public record ParsedSource(
            String canonicalUrl,
            ContentSourceType sourceType
    ) {
    }
}
