package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MarketingNotificationCampaignRepository
        extends JpaRepository<MarketingNotificationCampaign, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT campaign
            FROM MarketingNotificationCampaign campaign
            WHERE campaign.status IN (
                kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignStatus.PENDING,
                kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignStatus.PROCESSING
            )
            ORDER BY campaign.createdAt
            """)
    List<MarketingNotificationCampaign> findNextForUpdate(Pageable pageable);
}
