package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
final class NaverPlaceHtmlParser {

    private static final Pattern OG_TITLE = Pattern.compile(
            "<meta\\b(?=[^>]*\\bproperty=[\\\"']og:title[\\\"'])"
                    + "(?=[^>]*\\bcontent=[\\\"']([^\\\"']*)[\\\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JSON_LD_SCRIPT = Pattern.compile(
            "<script\\b(?=[^>]*\\btype\\s*=\\s*[\\\"']application/ld\\+json[\\\"'])"
                    + "[^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern SCRIPT = Pattern.compile(
            "<script\\b[^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern JSON_ROAD_ADDRESS = jsonString("roadAddress");
    private static final Pattern JSON_ADDRESS = jsonString("address");

    private final ObjectMapper objectMapper;

    NaverPlaceHtmlParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ParsedPlace parse(String html) {
        if (html == null || html.isBlank()) {
            throw new MetadataUnavailableException();
        }
        Optional<ParsedPlace> structured = parseStructuredScripts(html);
        if (structured.isPresent()) {
            return structured.get();
        }
        String name = cleanName(extractHtml(html, OG_TITLE));
        String address = firstNonBlank(
                extractJsonString(html, JSON_ROAD_ADDRESS),
                extractJsonString(html, JSON_ADDRESS)
        );
        if (name.isBlank() || address.isBlank()) {
            throw new MetadataUnavailableException();
        }
        return new ParsedPlace(name, address);
    }

    private Optional<ParsedPlace> parseStructuredScripts(String html) {
        Optional<ParsedPlace> jsonLd = parseScripts(html, JSON_LD_SCRIPT, true);
        return jsonLd.isPresent() ? jsonLd : parseScripts(html, SCRIPT, false);
    }

    private Optional<ParsedPlace> parseScripts(
            String html,
            Pattern scriptPattern,
            boolean requirePlaceType
    ) {
        Matcher scripts = scriptPattern.matcher(html);
        while (scripts.find()) {
            Optional<JsonNode> json = parseJson(scriptJson(scripts.group(1)));
            if (json.isEmpty()) {
                continue;
            }
            Optional<ParsedPlace> place = findPlace(json.get(), requirePlaceType);
            if (place.isPresent()) {
                return place;
            }
        }
        return Optional.empty();
    }

    private String scriptJson(String script) {
        String decoded = HtmlUtils.htmlUnescape(script).strip();
        int objectStart = decoded.indexOf('{');
        int arrayStart = decoded.indexOf('[');
        int start = firstJsonStart(objectStart, arrayStart);
        if (start < 0) {
            return "";
        }
        char opening = decoded.charAt(start);
        char closing = opening == '{' ? '}' : ']';
        int end = decoded.lastIndexOf(closing);
        return end < start ? "" : decoded.substring(start, end + 1);
    }

    private int firstJsonStart(int objectStart, int arrayStart) {
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
    }

    private Optional<JsonNode> parseJson(String json) {
        if (json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readTree(json));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<ParsedPlace> findPlace(JsonNode node, boolean requirePlaceType) {
        if (node.isObject() && (!requirePlaceType || isPlaceType(node.get("@type")))) {
            Optional<ParsedPlace> place = toPlace(node);
            if (place.isPresent()) {
                return place;
            }
        }
        for (JsonNode child : node) {
            Optional<ParsedPlace> place = findPlace(child, requirePlaceType);
            if (place.isPresent()) {
                return place;
            }
        }
        return Optional.empty();
    }

    private boolean isPlaceType(JsonNode type) {
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            for (JsonNode item : type) {
                if (isPlaceType(item)) {
                    return true;
                }
            }
            return false;
        }
        String value = type.asText("").toLowerCase();
        return value.contains("place")
                || value.contains("business")
                || value.contains("restaurant")
                || value.contains("cafe")
                || value.contains("store")
                || value.contains("lodging")
                || value.contains("touristattraction");
    }

    private Optional<ParsedPlace> toPlace(JsonNode node) {
        String name = cleanName(text(node.get("name")));
        String address = address(node);
        if (name.isBlank() || address.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedPlace(name, address));
    }

    private String address(JsonNode node) {
        String roadAddress = text(node.get("roadAddress"));
        if (!roadAddress.isBlank()) {
            return roadAddress;
        }
        JsonNode address = node.get("address");
        if (address == null) {
            return "";
        }
        if (address.isTextual()) {
            return address.asText().strip();
        }
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, address.get("addressRegion"));
        addIfPresent(parts, address.get("addressLocality"));
        addIfPresent(parts, address.get("streetAddress"));
        return String.join(" ", parts).strip();
    }

    private void addIfPresent(List<String> parts, JsonNode node) {
        String value = text(node);
        if (!value.isBlank()) {
            parts.add(value);
        }
    }

    private String text(JsonNode node) {
        return node == null || !node.isValueNode() ? "" : node.asText("").strip();
    }

    private static Pattern jsonString(String fieldName) {
        return Pattern.compile(
                "\\\"" + fieldName + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"",
                Pattern.CASE_INSENSITIVE
        );
    }

    private String extractHtml(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? HtmlUtils.htmlUnescape(matcher.group(1)).strip() : "";
    }

    private String extractJsonString(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        try {
            return objectMapper.readValue("\"" + matcher.group(1) + "\"", String.class).strip();
        } catch (RuntimeException exception) {
            return "";
        }
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
