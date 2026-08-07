package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, Long> {

    @Query("""
            SELECT COUNT(delivery)
            FROM NotificationDelivery delivery
            WHERE delivery.notification.receiver.id = :memberId
            """)
    long countByReceiverMemberId(@Param("memberId") Long memberId);

    @Query("""
            SELECT delivery
            FROM NotificationDelivery delivery
            JOIN FETCH delivery.notification
            JOIN FETCH delivery.pushDevice
            WHERE delivery.notification.receiver.id = :memberId
            ORDER BY delivery.id
            """)
    List<NotificationDelivery> findAllByReceiverMemberId(
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT delivery.id
            FROM NotificationDelivery delivery
            WHERE (
                delivery.status IN (
                    kr.omong.dulpick.domain.notification.domain.NotificationDeliveryStatus.PENDING,
                    kr.omong.dulpick.domain.notification.domain.NotificationDeliveryStatus.RETRY_PENDING
                )
                AND delivery.nextAttemptAt <= :now
            ) OR (
                delivery.status = kr.omong.dulpick.domain.notification.domain.NotificationDeliveryStatus.SENDING
                AND delivery.lastAttemptedAt <= :staleBefore
            )
            ORDER BY delivery.id
            """)
    List<Long> findClaimableIds(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT delivery
            FROM NotificationDelivery delivery
            JOIN FETCH delivery.notification
            JOIN FETCH delivery.pushDevice
            WHERE delivery.id = :deliveryId
            """)
    Optional<NotificationDelivery> findForUpdateById(
            @Param("deliveryId") Long deliveryId
    );
}
