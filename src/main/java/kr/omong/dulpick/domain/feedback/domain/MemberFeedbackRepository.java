package kr.omong.dulpick.domain.feedback.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MemberFeedbackRepository extends JpaRepository<MemberFeedback, Long> {

    Optional<MemberFeedback> findByMemberIdAndClientRequestId(
            Long memberId,
            String clientRequestId
    );

    long countByMemberIdAndCreatedAtGreaterThanEqual(Long memberId, Instant createdAt);

    @Query("""
            SELECT feedback.id
            FROM MemberFeedback feedback
            WHERE feedback.createdAt < :retainedAfter
            ORDER BY feedback.id
            """)
    List<Long> findExpiredIds(
            @Param("retainedAfter") Instant retainedAfter,
            Pageable pageable
    );
}
