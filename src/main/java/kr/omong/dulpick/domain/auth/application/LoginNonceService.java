package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.LoginNonce;
import kr.omong.dulpick.domain.auth.domain.LoginNonceRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.security.Sha256;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Service
@EnableConfigurationProperties(SocialLoginProperties.class)
public class LoginNonceService {

    private static final int NONCE_BYTES = 32;

    private final LoginNonceRepository loginNonceRepository;
    private final SocialLoginProperties properties;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public LoginNonceService(
            LoginNonceRepository loginNonceRepository,
            SocialLoginProperties properties,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.loginNonceRepository = loginNonceRepository;
        this.properties = properties;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public IssuedNonce issue(SocialProvider provider) {
        String rawNonce = generateNonce();
        Instant expiresAt = clock.instant().plus(properties.nonceTtl());
        LoginNonce nonce = LoginNonce.create(provider, Sha256.hex(rawNonce), expiresAt);
        loginNonceRepository.save(nonce);
        return new IssuedNonce(rawNonce, expiresAt);
    }

    @Transactional
    public void consume(SocialProvider provider, String rawNonce, String tokenNonce) {
        String expectedTokenNonce = expectedTokenNonce(provider, rawNonce);
        if (!constantTimeEquals(expectedTokenNonce, tokenNonce)) {
            throw new InvalidLoginNonceException();
        }

        LoginNonce nonce = loginNonceRepository
                .findByProviderAndNonceHash(provider, Sha256.hex(rawNonce))
                .filter(savedNonce -> savedNonce.isUsable(clock.instant()))
                .orElseThrow(InvalidLoginNonceException::new);
        nonce.use();
    }

    private String expectedTokenNonce(SocialProvider provider, String rawNonce) {
        if (provider == SocialProvider.APPLE) {
            return Sha256.base64Url(rawNonce);
        }
        return rawNonce;
    }

    private String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
