package kr.omong.dulpick.domain.place.infrastructure;

import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstagramCaptionMetadataParser {

    private static final Pattern DESCRIPTION = Pattern.compile(
            "(?s)^(?:(?<likes>[\\d,.]+(?:[KMB])?)\\s+likes?,\\s*)?"
                    + "(?:(?<comments>[\\d,.]+(?:[KMB])?)\\s+comments?\\s*)?"
                    + "-\\s*(?<username>[A-Za-z0-9._]+)\\s+on\\s+"
                    + "(?<publishedOn>[A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})"
                    + ":\\s*\"(?<caption>.*)\"\\.?$");
    private static final Pattern TITLE = Pattern.compile(
            "(?s)^(?<displayName>.*?)\\s+on Instagram:\\s*\"(?<caption>.*)\"$");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private InstagramCaptionMetadataParser() {
    }

    static Parsed parse(String rawTitle, String rawDescription, String fallbackCaption) {
        String title = decode(rawTitle);
        String description = decode(rawDescription);
        Matcher descriptionMatcher = DESCRIPTION.matcher(description);
        Matcher titleMatcher = TITLE.matcher(title);
        String caption = fallbackCaption == null ? "" : decode(fallbackCaption);
        String displayName = null;
        String username = null;
        LocalDate publishedOn = null;
        Long likes = null;
        Long comments = null;
        if (descriptionMatcher.matches()) {
            caption = decode(descriptionMatcher.group("caption"));
            username = descriptionMatcher.group("username");
            publishedOn = parseDate(descriptionMatcher.group("publishedOn"));
            likes = parseCount(descriptionMatcher.group("likes"));
            comments = parseCount(descriptionMatcher.group("comments"));
        }
        if (titleMatcher.matches()) {
            displayName = titleMatcher.group("displayName").strip();
            if (caption.isBlank()) {
                caption = decode(titleMatcher.group("caption"));
            }
        }
        String normalizedTitle = firstLine(caption);
        String content = remaining(caption);
        return new Parsed(
                displayName,
                username,
                publishedOn,
                likes,
                comments,
                normalizedTitle,
                content
        );
    }

    private static String decode(String value) {
        return value == null ? "" : HtmlUtils.htmlUnescape(value).strip();
    }

    private static String firstLine(String caption) {
        return caption.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");
    }

    private static String remaining(String caption) {
        String title = firstLine(caption);
        if (title.isBlank()) {
            return "";
        }
        int index = caption.indexOf(title);
        return caption.substring(index + title.length()).strip();
    }

    private static Long parseCount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace(",", "").toUpperCase(Locale.ROOT);
        double multiplier = 1;
        if (normalized.endsWith("K")) {
            multiplier = 1_000;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("M")) {
            multiplier = 1_000_000;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("B")) {
            multiplier = 1_000_000_000;
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return Math.round(Double.parseDouble(normalized) * multiplier);
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    record Parsed(
            String displayName,
            String username,
            LocalDate publishedOn,
            Long likeCount,
            Long commentCount,
            String title,
            String content
    ) {
    }
}
