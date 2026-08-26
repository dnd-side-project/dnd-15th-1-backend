package kr.omong.dulpick.domain.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailOptOutRepository extends JpaRepository<EmailOptOut, Long> {

    boolean existsByMemberIdAndCategory(Long memberId, String category);

    Optional<EmailOptOut> findByMemberIdAndCategory(Long memberId, String category);

    List<EmailOptOut> findAllByCategoryOrderByCreatedAtDesc(String category);
}
