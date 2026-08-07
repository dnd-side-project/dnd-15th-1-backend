package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.InvalidConnectionCodeException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ConnectionCodeNormalizer {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{6}$");

    public String normalize(String connectionCode) {
        if (connectionCode == null) {
            throw new InvalidConnectionCodeException();
        }
        String normalized = connectionCode.strip().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new InvalidConnectionCodeException();
        }
        return normalized;
    }
}
