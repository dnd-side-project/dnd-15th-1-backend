package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutbox;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
public class AppleRevocationOutboxWorker {

    private final AppleRevocationOutboxRepository outboxRepository;
    private final AppleAuthorizationService appleAuthorizationService;
    private final AppleRevocationOutboxProperties properties;
    private final Clock clock;

    public AppleRevocationOutboxWorker(
            AppleRevocationOutboxRepository outboxRepository,
            AppleAuthorizationService appleAuthorizationService,
            AppleRevocationOutboxProperties properties,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.appleAuthorizationService = appleAuthorizationService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void process(Long outboxId) {
        outboxRepository.findForUpdateById(outboxId).ifPresent(this::revoke);
    }

    private void revoke(AppleRevocationOutbox outbox) {
        try {
            appleAuthorizationService.revoke(
                    outbox.getEncryptedRefreshToken(),
                    outbox.getClientId()
            );
            outboxRepository.delete(outbox);
        } catch (AppleAuthorizationException exception) {
            outbox.scheduleRetry(
                    clock.instant(),
                    properties.initialRetryDelay(),
                    properties.maxRetryDelay()
            );
            log.warn(
                    "Apple token revocation scheduled for retry: outboxId={}, attempt={}",
                    outbox.getId(),
                    outbox.getAttemptCount()
            );
        }
    }
}
