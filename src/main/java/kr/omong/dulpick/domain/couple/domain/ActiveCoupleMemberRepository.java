package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ActiveCoupleMemberRepository
        extends JpaRepository<ActiveCoupleMember, Long> {

    Optional<ActiveCoupleMember> findByMemberId(Long memberId);

    List<ActiveCoupleMember> findAllByCoupleId(Long coupleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActiveCoupleMember> findForUpdateByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ActiveCoupleMember> findAllForUpdateByCoupleId(Long coupleId);
}
