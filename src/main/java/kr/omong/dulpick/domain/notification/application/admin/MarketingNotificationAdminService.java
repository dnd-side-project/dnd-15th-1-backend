package kr.omong.dulpick.domain.notification.application.admin;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notification.application.command.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.command.NotificationRequest;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class MarketingNotificationAdminService {

    private final MemberNotificationSettingsRepository settingsRepository;
    private final NotificationCreationService notificationCreationService;
    private final Clock clock;

    public MarketingNotificationAdminService(
            MemberNotificationSettingsRepository settingsRepository,
            NotificationCreationService notificationCreationService,
            Clock clock
    ) {
        this.settingsRepository = settingsRepository;
        this.notificationCreationService = notificationCreationService;
        this.clock = clock;
    }

    @Transactional
    public MarketingNotificationSendView send(MarketingNotificationCommand command) {
        Instant queuedAt = clock.instant();
        String campaignId = UUID.randomUUID().toString();
        int targetCount = settingsRepository
                .findMemberIdsWithMarketingEnabled(MemberStatus.ACTIVE)
                .stream()
                .map(memberId -> createNotification(memberId, command, campaignId, queuedAt))
                .mapToInt(created -> created ? 1 : 0)
                .sum();
        return new MarketingNotificationSendView(campaignId, targetCount, queuedAt);
    }

    private boolean createNotification(
            Long memberId,
            MarketingNotificationCommand command,
            String campaignId,
            Instant queuedAt
    ) {
        return notificationCreationService.createMarketingNotification(new NotificationRequest(
                memberId,
                NotificationType.MARKETING,
                command.title(),
                command.body(),
                NotificationRoute.NOTICE,
                campaignId,
                "MARKETING:" + campaignId,
                queuedAt
        ));
    }
}
