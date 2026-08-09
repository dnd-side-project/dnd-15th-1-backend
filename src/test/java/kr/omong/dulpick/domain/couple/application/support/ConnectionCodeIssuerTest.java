package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.ConnectionCodeGenerationException;
import kr.omong.dulpick.domain.couple.config.CoupleProperties;
import kr.omong.dulpick.domain.couple.domain.ConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeIssuedReason;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.couple.infrastructure.crypto.ConnectionCodeCipher;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        when(generator.generate()).thenReturn("AAAAA");
        when(repository.existsByCodeDigest(anyString())).thenReturn(true);

        assertThatThrownBy(() -> issuer.issue(
                Member.create(Instant.EPOCH),
                ConnectionCodeIssuedReason.ONBOARDING
        )).isInstanceOf(ConnectionCodeGenerationException.class);
    }

    @Test
    void normalizesShareBaseUrlOnce() {
        when(generator.generate()).thenReturn("ABCDE");
        when(repository.existsByCodeDigest(anyString())).thenReturn(false);
        when(cipher.encrypt("ABCDE")).thenReturn("encrypted");

        IssuedConnectionCode issued = issuer.issue(
                Member.create(Instant.EPOCH),
                ConnectionCodeIssuedReason.ONBOARDING
        );

        assertThat(issued.shareUrl()).isEqualTo("https://dulpick.app/connect?code=ABCDE");
    }

    @Test
    void replacesLegacyCodeWithCurrentFormatWhenRead() {
        Member member = Member.create(Instant.EPOCH);
        ConnectionCode legacyCode = ConnectionCode.issue(
                member,
                "legacy-digest",
                "legacy-encrypted",
                ConnectionCodeIssuedReason.ONBOARDING,
                Instant.EPOCH
        );
        when(cipher.decrypt("legacy-encrypted")).thenReturn("ABCDEF");
        when(generator.generate()).thenReturn("ABCDE");
        when(repository.existsByCodeDigest(anyString())).thenReturn(false);
        when(cipher.encrypt("ABCDE")).thenReturn("current-encrypted");

        IssuedConnectionCode issued = issuer.readCurrent(legacyCode);

        assertThat(legacyCode.getStatus()).isEqualTo(ConnectionCodeStatus.REVOKED);
        assertThat(issued.code()).isEqualTo("ABCDE");
        verify(repository).flush();
    }
}
