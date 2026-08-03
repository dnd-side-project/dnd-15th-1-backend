package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.properties.SocialLoginProperties;
import kr.omong.dulpick.domain.auth.domain.LoginNonce;
import kr.omong.dulpick.domain.auth.domain.LoginNonceRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

@Component
@EnableConfigurationProperties(SocialLoginProperties.class)
public class IssueNonceHandler {

    private static final int NONCE_BYTES = 32;

    private final LoginNonceRepository loginNonceRepository;
    private final SocialLoginProperties properties;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public IssueNonceHandler(
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
    public IssuedNonce handle(SocialProvider provider) {
        String rawNonce = generateNonce();
        Instant expiresAt = clock.instant().plus(properties.nonceTtl());
        LoginNonce nonce = LoginNonce.create(provider, Sha256.hex(rawNonce), expiresAt);
        loginNonceRepository.save(nonce);
        return new IssuedNonce(rawNonce, expiresAt);
    }

    private String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
