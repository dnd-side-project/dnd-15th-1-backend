package kr.omong.dulpick.domain.auth.application.scheduled;

import kr.omong.dulpick.domain.auth.application.properties.AuthenticationDataCleanupProperties;
import kr.omong.dulpick.domain.auth.domain.LoginNonce;
import kr.omong.dulpick.domain.auth.domain.LoginNonceRepository;
import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthenticationDataCleanupIntegrationTest {

    @Autowired
    private LoginNonceRepository loginNonceRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void removesStaleAuthenticationDataAndKeepsReplayEvidence() {
        Instant now = Instant.now();
        LoginNonce usedNonce = nonce("a", now.plus(Duration.ofDays(1)));
        usedNonce.use();
        LoginNonce expiredNonce = nonce("b", now.minusSeconds(1));
        LoginNonce activeNonce = nonce("c", now.plus(Duration.ofDays(1)));
        loginNonceRepository.save(usedNonce);
        loginNonceRepository.save(expiredNonce);
        loginNonceRepository.save(activeNonce);

        Member member = memberRepository.save(Member.create());
        RefreshToken expiredToken = token(member, "d", now.minusSeconds(1));
        RefreshToken revokedToken = token(member, "e", now.plus(Duration.ofDays(1)));
        revokedToken.revoke();
        RefreshToken rotatedToken = token(member, "f", now.plus(Duration.ofDays(1)));
        rotatedToken.rotate(hash("g"));
        RefreshToken activeToken = token(member, "h", now.plus(Duration.ofDays(1)));
        refreshTokenRepository.save(expiredToken);
        refreshTokenRepository.save(revokedToken);
        refreshTokenRepository.save(rotatedToken);
        refreshTokenRepository.save(activeToken);
        refreshTokenRepository.flush();

        AuthenticationDataCleanupService service = new AuthenticationDataCleanupService(
                loginNonceRepository,
                refreshTokenRepository,
                new AuthenticationDataCleanupProperties(
                        Duration.ofHours(1),
                        100,
                        10,
                        Duration.ZERO
                ),
                Clock.fixed(now.plusSeconds(10), ZoneOffset.UTC)
        );

        service.cleanup();

        assertThat(loginNonceRepository.findByProviderAndNonceHash(
                SocialProvider.KAKAO,
                hash("a")
        )).isEmpty();
        assertThat(loginNonceRepository.findByProviderAndNonceHash(
                SocialProvider.KAKAO,
                hash("b")
        )).isEmpty();
        assertThat(loginNonceRepository.findByProviderAndNonceHash(
                SocialProvider.KAKAO,
                hash("c")
        )).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash(hash("d"))).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(hash("e"))).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(hash("f"))).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash(hash("h"))).isPresent();
    }

    private LoginNonce nonce(String seed, Instant expiresAt) {
        return LoginNonce.create(SocialProvider.KAKAO, hash(seed), expiresAt);
    }

    private RefreshToken token(Member member, String seed, Instant expiresAt) {
        return RefreshToken.create(member, hash(seed), expiresAt);
    }

    private String hash(String seed) {
        return seed.repeat(64);
    }
}
