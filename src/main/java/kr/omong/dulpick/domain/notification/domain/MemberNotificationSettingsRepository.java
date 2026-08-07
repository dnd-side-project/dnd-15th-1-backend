package kr.omong.dulpick.domain.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberNotificationSettingsRepository
        extends JpaRepository<MemberNotificationSettings, Long> {
}
