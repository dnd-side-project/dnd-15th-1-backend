package kr.omong.dulpick.domain.notification.domain;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MemberNotificationSettingsRepository
        extends JpaRepository<MemberNotificationSettings, Long> {

    @Query("""
            SELECT settings.memberId
            FROM MemberNotificationSettings settings
            WHERE settings.marketingEnabled = TRUE
              AND settings.member.status = :status
            """)
    List<Long> findMemberIdsWithMarketingEnabled(MemberStatus status);

    @Query("""
            SELECT settings.memberId
            FROM MemberNotificationSettings settings
            WHERE settings.marketingEnabled = TRUE
              AND settings.member.status = :status
              AND settings.memberId > :afterMemberId
            ORDER BY settings.memberId
            """)
    List<Long> findMemberIdsWithMarketingEnabledAfter(
            MemberStatus status,
            Long afterMemberId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(settings)
            FROM MemberNotificationSettings settings
            WHERE settings.marketingEnabled = TRUE
              AND settings.member.status = :status
            """)
    long countMembersWithMarketingEnabled(MemberStatus status);
}
