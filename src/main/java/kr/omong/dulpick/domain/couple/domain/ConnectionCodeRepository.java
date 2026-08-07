package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ConnectionCodeRepository extends JpaRepository<ConnectionCode, Long> {

    boolean existsByCodeDigest(String codeDigest);

    List<ConnectionCode> findAllByMemberId(Long memberId);

    Optional<ConnectionCode> findByMemberIdAndStatus(
            Long memberId,
            ConnectionCodeStatus status
    );

    Optional<ConnectionCode> findByCodeDigestAndStatus(
            String codeDigest,
            ConnectionCodeStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConnectionCode> findForUpdateByCodeDigest(String codeDigest);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ConnectionCode> findAllForUpdateByMemberIdAndStatus(
            Long memberId,
            ConnectionCodeStatus status
    );
}
