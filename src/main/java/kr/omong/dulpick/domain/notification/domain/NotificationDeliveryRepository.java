package kr.omong.dulpick.domain.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, Long> {

    @Query("""
            SELECT COUNT(delivery)
            FROM NotificationDelivery delivery
            WHERE delivery.notification.receiver.id = :memberId
            """)
    long countByReceiverMemberId(@Param("memberId") Long memberId);
}
