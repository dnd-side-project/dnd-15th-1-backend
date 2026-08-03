package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.global.security.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoginNonceServiceTest {

    @Autowired
    private LoginNonceService loginNonceService;

    @Autowired
    private AuthCommandService authCommandService;

    @Autowired
    private Clock clock;

    @Test
    void issuesNonceWithTenMinuteLifetimeUsingKoreaTimeZone() {
        Instant earliestExpiry = clock.instant().plus(Duration.ofMinutes(10));

        IssuedNonce issuedNonce = authCommandService.issueNonce(SocialProvider.KAKAO);

        Instant latestExpiry = clock.instant().plus(Duration.ofMinutes(10));
        assertThat(clock.getZone().getId()).isEqualTo("Asia/Seoul");
        assertThat(issuedNonce.expiresAt())
                .isBetween(earliestExpiry, latestExpiry);
    }

    @Test
    void consumesGoogleNonceOnlyOnce() {
        IssuedNonce issuedNonce = authCommandService.issueNonce(SocialProvider.GOOGLE);

        loginNonceService.consume(
                SocialProvider.GOOGLE,
                issuedNonce.nonce(),
                issuedNonce.nonce()
        );

        assertThatThrownBy(() -> loginNonceService.consume(
                SocialProvider.GOOGLE,
                issuedNonce.nonce(),
                issuedNonce.nonce()
        )).isInstanceOf(InvalidLoginNonceException.class);
    }

    @Test
    void consumesAppleNonceOnlyOnce() {
        IssuedNonce issuedNonce = authCommandService.issueNonce(SocialProvider.APPLE);
        String tokenNonce = Sha256.hex(issuedNonce.nonce());

        loginNonceService.consume(SocialProvider.APPLE, issuedNonce.nonce(), tokenNonce);

        assertThatThrownBy(() -> loginNonceService.consume(
                SocialProvider.APPLE,
                issuedNonce.nonce(),
                tokenNonce
        )).isInstanceOf(InvalidLoginNonceException.class);
    }

    @Test
    void rejectsMismatchedKakaoNonce() {
        IssuedNonce issuedNonce = authCommandService.issueNonce(SocialProvider.KAKAO);

        assertThatThrownBy(() -> loginNonceService.consume(
                SocialProvider.KAKAO,
                issuedNonce.nonce(),
                "different-nonce"
        )).isInstanceOf(InvalidLoginNonceException.class);
    }
}
