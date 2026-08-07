package kr.omong.dulpick.domain.couple.domain;

import java.util.regex.Pattern;

public final class ConnectionCodeFormat {

    public static final int LENGTH = 5;
    public static final String NORMALIZED_PATTERN = "^[A-Z]{5}$";
    public static final String INPUT_PATTERN = "^[A-Za-z]{5}$";

    private static final Pattern PATTERN = Pattern.compile(NORMALIZED_PATTERN);

    private ConnectionCodeFormat() {
    }

    public static boolean isCurrent(String code) {
        return code != null && PATTERN.matcher(code).matches();
    }
}
