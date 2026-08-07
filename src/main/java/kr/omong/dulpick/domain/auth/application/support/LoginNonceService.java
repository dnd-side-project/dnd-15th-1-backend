package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.application.exception.InvalidLoginNonceException;
import kr.omong.dulpick.domain.auth.domain.LoginNonce;
import kr.omong.dulpick.domain.auth.domain.LoginNonceRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;

@Service
public class LoginNonceService {

    private final LoginNonceRepository loginNonceRepository;
    private final Clock clock;

    public LoginNonceService(
            LoginNonceRepository loginNonceRepository,
            Clock clock
    ) {
        this.loginNonceRepository = loginNonceRepository;
        this.clock = clock;
    }

    @Transactional
    public void consume(SocialProvider provider, String rawNonce, String tokenNonce) {
        String expectedTokenNonce = expectedTokenNonce(provider, rawNonce);
        if (!constantTimeEquals(expectedTokenNonce, tokenNonce)) {
            throw new InvalidLoginNonceException();
        }

        Instant consumedAt = clock.instant();
        LoginNonce nonce = loginNonceRepository
                .findByProviderAndNonceHash(provider, Sha256.hex(rawNonce))
                .filter(savedNonce -> savedNonce.isUsable(consumedAt))
                .orElseThrow(InvalidLoginNonceException::new);
        nonce.use(consumedAt);
    }

    private String expectedTokenNonce(SocialProvider provider, String rawNonce) {
        if (provider == SocialProvider.APPLE) {
            return Sha256.hex(rawNonce);
        }
        return rawNonce;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
