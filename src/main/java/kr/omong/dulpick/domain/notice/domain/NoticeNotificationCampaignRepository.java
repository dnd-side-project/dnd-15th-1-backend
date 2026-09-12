package kr.omong.dulpick.domain.notice.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface NoticeNotificationCampaignRepository
        extends JpaRepository<NoticeNotificationCampaign, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT campaign
            FROM NoticeNotificationCampaign campaign
            WHERE campaign.status IN (
                kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaignStatus.PENDING,
                kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaignStatus.PROCESSING
            )
            ORDER BY campaign.createdAt
            """)
    List<NoticeNotificationCampaign> findNextForUpdate(Pageable pageable);

    java.util.Optional<NoticeNotificationCampaign> findByNoticeId(Long noticeId);
}
