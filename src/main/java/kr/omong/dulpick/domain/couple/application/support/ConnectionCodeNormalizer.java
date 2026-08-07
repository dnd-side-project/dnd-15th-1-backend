package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.InvalidConnectionCodeException;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeFormat;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConnectionCodeNormalizer {

    public String normalize(String connectionCode) {
        if (connectionCode == null) {
            throw new InvalidConnectionCodeException();
        }
        String normalized = connectionCode.strip().toUpperCase(Locale.ROOT);
        if (!ConnectionCodeFormat.isCurrent(normalized)) {
            throw new InvalidConnectionCodeException();
        }
        return normalized;
    }
}
