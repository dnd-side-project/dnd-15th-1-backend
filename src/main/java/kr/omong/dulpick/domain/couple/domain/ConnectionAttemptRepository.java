package kr.omong.dulpick.domain.couple.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;

public interface ConnectionAttemptRepository extends JpaRepository<ConnectionAttempt, Long> {

    long countByMemberIdAndActionAndCreatedAtGreaterThanEqual(
            Long memberId,
            ConnectionAttempt.Action action,
            Instant since
    );

    long countByMemberIdAndActionInAndCreatedAtGreaterThanEqual(
            Long memberId,
            Collection<ConnectionAttempt.Action> actions,
            Instant since
    );

    long countByMemberIdAndOutcomeAndCreatedAtGreaterThanEqual(
            Long memberId,
            ConnectionAttempt.Outcome outcome,
            Instant since
    );

    long countByIpHashAndOutcomeAndCreatedAtGreaterThanEqual(
            String ipHash,
            ConnectionAttempt.Outcome outcome,
            Instant since
    );

    long deleteByCreatedAtBefore(Instant cutoff);

    @Transactional
    long deleteAllByMemberId(Long memberId);
}
