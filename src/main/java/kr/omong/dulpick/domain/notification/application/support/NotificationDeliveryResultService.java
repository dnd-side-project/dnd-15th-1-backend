package kr.omong.dulpick.domain.notification.application.support;

import kr.omong.dulpick.domain.notification.config.FcmProperties;
import kr.omong.dulpick.domain.notification.domain.NotificationDelivery;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.infrastructure.PushSendException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class NotificationDeliveryResultService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final FcmProperties properties;
    private final Clock clock;

    public NotificationDeliveryResultService(
            NotificationDeliveryRepository deliveryRepository,
            FcmProperties properties,
            Clock clock
    ) {
        this.deliveryRepository = deliveryRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void markSent(Long deliveryId, String providerMessageId) {
        deliveryRepository.findForUpdateById(deliveryId)
                .ifPresent(delivery -> delivery.markSent(
                        providerMessageId,
                        clock.instant()
                ));
    }

    @Transactional
    public void markFailed(Long deliveryId, PushSendException exception) {
        deliveryRepository.findForUpdateById(deliveryId)
                .ifPresent(delivery -> handleFailure(delivery, exception));
    }

    private void handleFailure(
            NotificationDelivery delivery,
            PushSendException exception
    ) {
        delivery.handleFailure(
                exception.getErrorCode(),
                exception.isRetryable(),
                exception.isInvalidRegistration(),
                clock.instant(),
                properties.maxAttempts(),
                properties.initialRetryDelay(),
                properties.maxRetryDelay()
        );
    }
}
