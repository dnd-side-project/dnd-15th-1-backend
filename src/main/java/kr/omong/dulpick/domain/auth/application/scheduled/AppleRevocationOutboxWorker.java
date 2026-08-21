package kr.omong.dulpick.domain.auth.application.scheduled;

import kr.omong.dulpick.domain.auth.application.properties.AppleRevocationOutboxProperties;
import kr.omong.dulpick.domain.auth.application.support.AppleAuthorizationService;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutbox;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationStatus;
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
        if (outbox.getStatus() != AppleRevocationStatus.PENDING
                || outbox.getAttemptCount() >= properties.maxAttempts()) {
            return;
        }
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
                    properties.maxRetryDelay(),
                    properties.maxAttempts()
            );
            if (outbox.getStatus() == AppleRevocationStatus.FAILED) {
                log.error(
                        "Apple token revocation permanently failed: outboxId={}, attempts={}",
                        outbox.getId(),
                        outbox.getAttemptCount()
                );
                return;
            }
            log.warn("Apple token revocation scheduled for retry: outboxId={}, attempt={}",
                    outbox.getId(), outbox.getAttemptCount());
        }
    }
}
