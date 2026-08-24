package kr.omong.dulpick.domain.notification.domain;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberNotificationSettingsRepository
        extends JpaRepository<MemberNotificationSettings, Long> {

    @Query("""
            SELECT settings.memberId
            FROM MemberNotificationSettings settings
            WHERE settings.marketingEnabled = TRUE
              AND settings.member.status = :status
            """)
    List<Long> findMemberIdsWithMarketingEnabled(MemberStatus status);
}
