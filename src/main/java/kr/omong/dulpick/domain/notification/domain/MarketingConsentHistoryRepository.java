package kr.omong.dulpick.domain.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingConsentHistoryRepository
        extends JpaRepository<MarketingConsentHistory, Long> {

    long countByMemberId(Long memberId);
}
