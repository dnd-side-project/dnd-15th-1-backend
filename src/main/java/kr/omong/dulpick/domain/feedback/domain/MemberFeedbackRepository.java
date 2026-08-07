package kr.omong.dulpick.domain.feedback.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface MemberFeedbackRepository extends JpaRepository<MemberFeedback, Long> {

    Optional<MemberFeedback> findByMemberIdAndClientRequestId(
            Long memberId,
            String clientRequestId
    );

    long countByMemberIdAndCreatedAtGreaterThanEqual(Long memberId, Instant createdAt);
}
