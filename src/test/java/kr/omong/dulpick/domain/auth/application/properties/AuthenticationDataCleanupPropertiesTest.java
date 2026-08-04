package kr.omong.dulpick.domain.auth.application.properties;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AuthenticationDataCleanupPropertiesTest {

    @Test
    void rejectsInvalidCleanupConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ZERO,
                100,
                10,
                Duration.ofDays(1)
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofHours(1),
                0,
                10,
                Duration.ofDays(1)
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofHours(1),
                100,
                0,
                Duration.ofDays(1)
        ));
        assertThatIllegalArgumentException().isThrownBy(() -> properties(
                Duration.ofHours(1),
                100,
                10,
                Duration.ofDays(-1)
        ));
    }

    private AuthenticationDataCleanupProperties properties(
            Duration fixedDelay,
            int batchSize,
            int maxBatchesPerRun,
            Duration revokedRetention
    ) {
        return new AuthenticationDataCleanupProperties(
                fixedDelay,
                batchSize,
                maxBatchesPerRun,
                revokedRetention
        );
    }
}
