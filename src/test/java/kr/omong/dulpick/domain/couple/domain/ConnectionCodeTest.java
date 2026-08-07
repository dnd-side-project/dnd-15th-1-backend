package kr.omong.dulpick.domain.couple.domain;

import kr.omong.dulpick.domain.couple.domain.exception.ConnectionCodeNotActiveException;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectionCodeTest {

    private static final Instant NOW = Instant.parse("2026-08-07T00:00:00Z");

    @Test
    void rejectsRepeatedUseAndRevocation() {
        ConnectionCode usedCode = code();
        usedCode.use(NOW);
        ConnectionCode revokedCode = code();
        revokedCode.revoke(NOW);

        assertThat(usedCode.getStatus()).isEqualTo(ConnectionCodeStatus.USED);
        assertThat(revokedCode.getStatus()).isEqualTo(ConnectionCodeStatus.REVOKED);
        assertThatThrownBy(() -> usedCode.use(NOW))
                .isInstanceOf(ConnectionCodeNotActiveException.class);
        assertThatThrownBy(() -> revokedCode.revoke(NOW))
                .isInstanceOf(ConnectionCodeNotActiveException.class);
    }

    private ConnectionCode code() {
        return ConnectionCode.issue(
                Member.create(Instant.EPOCH),
                "digest",
                "encrypted",
                ConnectionCodeIssuedReason.ONBOARDING,
                NOW
        );
    }
}
