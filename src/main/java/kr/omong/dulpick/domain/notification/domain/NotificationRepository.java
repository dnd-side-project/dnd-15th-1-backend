package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.receiver.id = :memberId
              AND (:cursorId IS NULL OR notification.id < :cursorId)
            ORDER BY notification.id DESC
            """)
    List<Notification> findPage(
            @Param("memberId") Long memberId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(notification)
            FROM Notification notification
            WHERE notification.receiver.id = :memberId
              AND notification.readAt IS NULL
            """)
    long countUnreadByMemberId(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.id = :notificationId
              AND notification.receiver.id = :memberId
            """)
    Optional<Notification> findOwnedForUpdate(
            @Param("notificationId") Long notificationId,
            @Param("memberId") Long memberId
    );

    @Query("""
            SELECT CASE WHEN COUNT(notification) > 0 THEN TRUE ELSE FALSE END
            FROM Notification notification
            WHERE notification.receiver.id = :receiverId
              AND notification.deduplicationKey = :deduplicationKey
            """)
    boolean existsByReceiverIdAndDeduplicationKey(
            @Param("receiverId") Long receiverId,
            @Param("deduplicationKey") String deduplicationKey
    );

    @Modifying
    @Query("""
            UPDATE Notification notification
            SET notification.readAt = :readAt
            WHERE notification.receiver.id = :memberId
              AND notification.readAt IS NULL
            """)
    int markAllRead(
            @Param("memberId") Long memberId,
            @Param("readAt") Instant readAt
    );

    @Query("""
            SELECT notification.id
            FROM Notification notification
            WHERE notification.createdAt < :retainedAfter
            ORDER BY notification.id
            """)
    List<Long> findExpiredIds(
            @Param("retainedAfter") Instant retainedAfter,
            Pageable pageable
    );
}
