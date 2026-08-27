package kr.omong.dulpick.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("notification.maintenance")
public record NotificationMaintenanceProperties(
        Duration fixedDelay,
        Duration notificationRetention,
        int batchSize,
        int maxBatchesPerRun
) {

    public NotificationMaintenanceProperties {
        requirePositive(fixedDelay, "fixed-delay");
        requirePositive(notificationRetention, "notification-retention");
        if (batchSize <= 0 || maxBatchesPerRun <= 0) {
            throw new IllegalArgumentException(
                    "notification maintenance batch values must be positive"
            );
        }
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "notification.maintenance." + name + " must be positive"
            );
        }
    }
}
