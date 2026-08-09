package kr.omong.dulpick.domain.notification.application.scheduled;

import kr.omong.dulpick.domain.feedback.domain.MemberFeedbackRepository;
import kr.omong.dulpick.domain.notification.config.NotificationMaintenanceProperties;
import kr.omong.dulpick.domain.notification.domain.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class NotificationDataCleanupService {

    private final NotificationRepository notificationRepository;
    private final MemberFeedbackRepository feedbackRepository;
    private final NotificationMaintenanceProperties properties;
    private final Clock clock;

    public NotificationDataCleanupService(
            NotificationRepository notificationRepository,
            MemberFeedbackRepository feedbackRepository,
            NotificationMaintenanceProperties properties,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.feedbackRepository = feedbackRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${notification.maintenance.fixed-delay:1h}",
            fixedDelayString = "${notification.maintenance.fixed-delay:1h}"
    )
    @Transactional
    public void cleanUp() {
        Instant now = clock.instant();
        deleteExpired(
                () -> notificationRepository.findExpiredIds(
                        now.minus(properties.notificationRetention()),
                        PageRequest.of(0, properties.batchSize())
                ),
                notificationRepository::deleteAllByIdInBatch
        );
        deleteExpired(
                () -> feedbackRepository.findExpiredIds(
                        now.minus(properties.feedbackRetention()),
                        PageRequest.of(0, properties.batchSize())
                ),
                feedbackRepository::deleteAllByIdInBatch
        );
    }

    private void deleteExpired(
            Supplier<List<Long>> findIds,
            Consumer<Iterable<Long>> deleteIds
    ) {
        for (int batch = 0; batch < properties.maxBatchesPerRun(); batch++) {
            List<Long> ids = findIds.get();
            if (ids.isEmpty()) {
                return;
            }
            deleteIds.accept(ids);
        }
    }
}
