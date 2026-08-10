package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NaverPlaceHtmlParser {

    private static final Pattern OG_TITLE = Pattern.compile(
            "<meta\\b(?=[^>]*\\bproperty=[\\\"']og:title[\\\"'])"
                    + "(?=[^>]*\\bcontent=[\\\"']([^\\\"']*)[\\\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JSON_NAME = jsonString("name");
    private static final Pattern JSON_ROAD_ADDRESS = jsonString("roadAddress");
    private static final Pattern JSON_ADDRESS = jsonString("address");

    ParsedPlace parse(String html) {
        if (html == null || html.isBlank()) {
            throw new MetadataUnavailableException();
        }
        String name = cleanName(firstNonBlank(extract(html, OG_TITLE), extract(html, JSON_NAME)));
        String address = firstNonBlank(
                extract(html, JSON_ROAD_ADDRESS),
                extract(html, JSON_ADDRESS)
        );
        if (name.isBlank() || address.isBlank()) {
            throw new MetadataUnavailableException();
        }
        return new ParsedPlace(name, address);
    }

    private static Pattern jsonString(String fieldName) {
        return Pattern.compile(
                "\\\"" + fieldName + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"",
                Pattern.CASE_INSENSITIVE
        );
    }

    private String extract(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        return HtmlUtils.htmlUnescape(matcher.group(1))
                .replace("\\/", "/")
                .strip();
    }

    private String cleanName(String value) {
        return value.replaceAll("[\\p{Cc}]", " ")
                .replaceFirst("\\s*[:|\\-]\\s*네이버.*$", "")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private String firstNonBlank(String first, String second) {
        return first.isBlank() ? second : first;
    }

    record ParsedPlace(String name, String address) {
    }
}
