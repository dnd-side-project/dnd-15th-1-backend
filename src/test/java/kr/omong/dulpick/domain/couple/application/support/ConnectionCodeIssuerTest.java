package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.ConnectionCodeGenerationException;
import kr.omong.dulpick.domain.couple.config.CoupleProperties;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeIssuedReason;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.infrastructure.crypto.ConnectionCodeCipher;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionCodeIssuerTest {

    private final ConnectionCodeRepository repository = mock(ConnectionCodeRepository.class);
    private final ConnectionCodeGenerator generator = mock(ConnectionCodeGenerator.class);
    private final ConnectionCodeCipher cipher = mock(ConnectionCodeCipher.class);
    private final ConnectionCodeIssuer issuer = new ConnectionCodeIssuer(
            repository,
            generator,
            cipher,
            new CoupleProperties("https://dulpick.app///", "encryption-key"),
            Clock.systemUTC()
    );

    @Test
    void failsWithStableExceptionAfterUniqueCodeAttemptsAreExhausted() {
        when(generator.generate()).thenReturn("AAAAAA");
        when(repository.existsByCodeDigest(anyString())).thenReturn(true);

        assertThatThrownBy(() -> issuer.issue(
                Member.create(Instant.EPOCH),
                ConnectionCodeIssuedReason.ONBOARDING
        )).isInstanceOf(ConnectionCodeGenerationException.class);
    }

    @Test
    void normalizesShareBaseUrlOnce() {
        when(generator.generate()).thenReturn("ABCDEF");
        when(repository.existsByCodeDigest(anyString())).thenReturn(false);
        when(cipher.encrypt("ABCDEF")).thenReturn("encrypted");

        IssuedConnectionCode issued = issuer.issue(
                Member.create(Instant.EPOCH),
                ConnectionCodeIssuedReason.ONBOARDING
        );

        assertThat(issued.shareUrl()).isEqualTo("https://dulpick.app/connect?code=ABCDEF");
    }
}
