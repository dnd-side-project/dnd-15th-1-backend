package kr.omong.dulpick.domain.place.application;

import java.util.List;

public final class PopularContentTags {

    private static final List<String> VALUES = List.of("성수", "강남", "을지로");

    private PopularContentTags() {
    }

    public static List<String> values() {
        return VALUES;
    }
}
