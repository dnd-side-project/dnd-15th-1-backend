package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.notification.infrastructure.PushMessageProvider;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationEncryptionException;
import kr.omong.dulpick.domain.notification.infrastructure.PushSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "notification.fcm.enabled", havingValue = "true")
public class NotificationDeliveryWorker {

    private final NotificationDeliveryClaimService claimService;
    private final NotificationDeliveryResultService resultService;
    private final PushRegistrationCipher registrationCipher;
    private final PushMessageProvider pushMessageProvider;

    public NotificationDeliveryWorker(
            NotificationDeliveryClaimService claimService,
            NotificationDeliveryResultService resultService,
            PushRegistrationCipher registrationCipher,
            PushMessageProvider pushMessageProvider
    ) {
        this.claimService = claimService;
        this.resultService = resultService;
        this.registrationCipher = registrationCipher;
        this.pushMessageProvider = pushMessageProvider;
    }

    public void process(Long deliveryId) {
        claimService.claim(deliveryId).ifPresent(this::send);
    }

    private void send(NotificationDeliveryTask task) {
        try {
            String registrationId = registrationCipher.decrypt(
                    task.encryptedRegistrationId()
            );
            String providerMessageId = pushMessageProvider.send(
                    registrationId,
                    task.message()
            );
            resultService.markSent(task.deliveryId(), providerMessageId);
        } catch (PushSendException exception) {
            resultService.markFailed(task.deliveryId(), exception);
            logFailure(task.deliveryId(), exception);
        } catch (PushRegistrationEncryptionException exception) {
            PushSendException failure = new PushSendException(
                    "TOKEN_DECRYPTION_FAILED",
                    false,
                    false,
                    exception
            );
            resultService.markFailed(task.deliveryId(), failure);
            log.error("Push token decryption failed: deliveryId={}", task.deliveryId());
        }
    }

    private void logFailure(Long deliveryId, PushSendException exception) {
        if (exception.isRetryable()) {
            log.warn(
                    "Push delivery scheduled for retry: deliveryId={}, code={}",
                    deliveryId,
                    exception.getErrorCode()
            );
            return;
        }
        log.warn(
                "Push delivery failed permanently: deliveryId={}, code={}",
                deliveryId,
                exception.getErrorCode()
        );
    }
}
