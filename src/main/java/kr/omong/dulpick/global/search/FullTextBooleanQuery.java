package kr.omong.dulpick.global.search;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class FullTextBooleanQuery {

    private FullTextBooleanQuery() {
    }

    public static String from(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "+__no_match__";
        }
        String sanitized = rawQuery.replaceAll("[+\\-><()~*\"@\\\\]", " ")
                .strip()
                .replaceAll("\\s+", " ");
        if (sanitized.isBlank()) {
            return "+__no_match__";
        }
        String booleanQuery = Arrays.stream(sanitized.split(" "))
                .filter(token -> token.length() >= 2)
                .map(token -> "+" + token)
                .collect(Collectors.joining(" "));
        return booleanQuery.isBlank() ? "+__no_match__" : booleanQuery;
    }
}
