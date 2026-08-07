package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CoupleRepository extends JpaRepository<Couple, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Couple> findForUpdateById(Long id);
}
