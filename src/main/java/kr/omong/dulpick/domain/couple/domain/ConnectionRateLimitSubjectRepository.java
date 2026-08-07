package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ConnectionRateLimitSubjectRepository
        extends JpaRepository<ConnectionRateLimitSubject, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConnectionRateLimitSubject> findForUpdateByMemberId(Long memberId);

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO connection_rate_limit_subjects
                        (member_id, blocked_until, updated_at)
                    VALUES (:memberId, NULL, :updatedAt)
                    """,
            nativeQuery = true
    )
    int createIfAbsent(
            @Param("memberId") Long memberId,
            @Param("updatedAt") Instant updatedAt
    );
}
