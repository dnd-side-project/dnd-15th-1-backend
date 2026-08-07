package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.notification.config.FcmProperties;
import kr.omong.dulpick.domain.notification.domain.Notification;
import kr.omong.dulpick.domain.notification.domain.NotificationDelivery;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;
import kr.omong.dulpick.domain.notification.infrastructure.PushMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class NotificationDeliveryClaimService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final FcmProperties properties;
    private final Clock clock;

    public NotificationDeliveryClaimService(
            NotificationDeliveryRepository deliveryRepository,
            FcmProperties properties,
            Clock clock
    ) {
        this.deliveryRepository = deliveryRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Optional<NotificationDeliveryTask> claim(Long deliveryId) {
        NotificationDelivery delivery = deliveryRepository.findForUpdateById(deliveryId)
                .orElse(null);
        Instant now = clock.instant();
        if (delivery == null || !delivery.canClaim(now, properties.sendingTimeout())) {
            return Optional.empty();
        }
        if (delivery.getProvider() != PushProviderType.FCM) {
            delivery.failWithoutAttempt("UNSUPPORTED_PROVIDER", now);
            return Optional.empty();
        }
        if (delivery.getPushDeviceStatus() != PushDeviceStatus.ACTIVE) {
            delivery.failWithoutAttempt("DEVICE_INACTIVE", now);
            return Optional.empty();
        }
        delivery.claim(now);
        return Optional.of(toTask(delivery));
    }

    private NotificationDeliveryTask toTask(NotificationDelivery delivery) {
        Notification notification = delivery.getNotification();
        return new NotificationDeliveryTask(
                delivery.getId(),
                delivery.getEncryptedRegistrationId(),
                new PushMessage(
                        notification.getId(),
                        notification.getType(),
                        notification.getTitle(),
                        notification.getBody(),
                        notification.getRoute(),
                        notification.getReferenceId()
                )
        );
    }
}
