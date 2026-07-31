package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppleRevocationOutboxRepository
        extends JpaRepository<AppleRevocationOutbox, Long> {

    @Query("""
            SELECT outbox.id
            FROM AppleRevocationOutbox outbox
            WHERE outbox.nextAttemptAt <= :now
            ORDER BY outbox.id
            """)
    List<Long> findRetryableIds(@Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT outbox
            FROM AppleRevocationOutbox outbox
            WHERE outbox.id = :id
            """)
    Optional<AppleRevocationOutbox> findForUpdateById(@Param("id") Long id);
}
