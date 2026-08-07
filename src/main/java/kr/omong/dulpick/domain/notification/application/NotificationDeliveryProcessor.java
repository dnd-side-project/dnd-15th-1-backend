package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.notification.config.FcmProperties;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "notification.fcm.enabled", havingValue = "true")
public class NotificationDeliveryProcessor {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryWorker deliveryWorker;
    private final FcmProperties properties;
    private final Clock clock;

    public NotificationDeliveryProcessor(
            NotificationDeliveryRepository deliveryRepository,
            NotificationDeliveryWorker deliveryWorker,
            FcmProperties properties,
            Clock clock
    ) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryWorker = deliveryWorker;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${notification.fcm.process-delay:10s}",
            fixedDelayString = "${notification.fcm.process-delay:10s}"
    )
    public void process() {
        Instant now = clock.instant();
        deliveryRepository.findClaimableIds(
                now,
                now.minus(properties.sendingTimeout()),
                PageRequest.of(0, properties.batchSize())
        ).forEach(deliveryWorker::process);
    }
}
