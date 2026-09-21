package kr.omong.dulpick.domain.member.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Member> findForUpdateById(Long id);

    @Query("""
            SELECT member.id
            FROM Member member
            WHERE member.status = :status AND member.id > :afterMemberId
            ORDER BY member.id
            """)
    java.util.List<Long> findIdsByStatusAfter(
            @Param("status") MemberStatus status,
            @Param("afterMemberId") Long afterMemberId,
            Pageable pageable
    );

    long countByStatus(MemberStatus status);
}
