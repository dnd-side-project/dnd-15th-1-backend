package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ContentSaveCounterRepository
        extends JpaRepository<ContentSaveCounter, ContentSaveCounterId> {

    @Modifying
    @Query(value = """
            INSERT INTO couple_content_save_counters (
                couple_id,
                saver_member_id,
                save_count,
                last_notified_milestone,
                created_at,
                updated_at
            ) VALUES (
                :coupleId,
                :saverMemberId,
                1,
                0,
                :createdAt,
                :savedAt
            ) ON DUPLICATE KEY UPDATE
                save_count = save_count + 1,
                updated_at = :savedAt
            """, nativeQuery = true)
    int increase(
            @Param("coupleId") Long coupleId,
            @Param("saverMemberId") Long saverMemberId,
            @Param("createdAt") Instant createdAt,
            @Param("savedAt") Instant savedAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT counter
            FROM ContentSaveCounter counter
            WHERE counter.coupleId = :coupleId
              AND counter.saverMemberId = :saverMemberId
            """)
    Optional<ContentSaveCounter> findForUpdate(
            @Param("coupleId") Long coupleId,
            @Param("saverMemberId") Long saverMemberId
    );
}
