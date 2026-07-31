package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutbox;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppleRevocationOutboxWorkerTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    private final AppleRevocationOutboxRepository outboxRepository =
            mock(AppleRevocationOutboxRepository.class);
    private final AppleAuthorizationService appleAuthorizationService =
            mock(AppleAuthorizationService.class);
    private final AppleRevocationOutboxProperties properties =
            new AppleRevocationOutboxProperties(
                    Duration.ofMinutes(1),
                    20,
                    Duration.ofMinutes(1),
                    Duration.ofHours(1)
            );
    private final AppleRevocationOutboxWorker worker = new AppleRevocationOutboxWorker(
            outboxRepository,
            appleAuthorizationService,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void deletesOutboxAfterSuccessfulRevocation() {
        AppleRevocationOutbox outbox = outbox();
        when(outboxRepository.findForUpdateById(1L)).thenReturn(Optional.of(outbox));

        worker.process(1L);

        verify(appleAuthorizationService).revoke(
                "encrypted-refresh-token",
                "com.dulpick.app"
        );
        verify(outboxRepository).delete(outbox);
    }

    @Test
    void schedulesRetryWithoutExposingTokenWhenRevocationFails() {
        AppleRevocationOutbox outbox = outbox();
        when(outboxRepository.findForUpdateById(1L)).thenReturn(Optional.of(outbox));
        doThrow(new AppleAuthorizationException("sensitive-token"))
                .when(appleAuthorizationService)
                .revoke("encrypted-refresh-token", "com.dulpick.app");

        worker.process(1L);

        assertThat(outbox.getAttemptCount()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        verify(outboxRepository, never()).delete(outbox);
    }

    private AppleRevocationOutbox outbox() {
        return AppleRevocationOutbox.create(
                1L,
                "encrypted-refresh-token",
                "com.dulpick.app",
                NOW.minusSeconds(1)
        );
    }
}
