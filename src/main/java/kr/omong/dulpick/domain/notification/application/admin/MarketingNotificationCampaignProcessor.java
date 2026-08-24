package kr.omong.dulpick.domain.notification.application.admin;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notification.application.command.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.command.NotificationRequest;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaign;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class MarketingNotificationCampaignProcessor {

    private static final int BATCH_SIZE = 100;

    private final MarketingNotificationCampaignRepository campaignRepository;
    private final MemberNotificationSettingsRepository settingsRepository;
    private final NotificationCreationService notificationCreationService;
    private final Clock clock;

    public MarketingNotificationCampaignProcessor(
            MarketingNotificationCampaignRepository campaignRepository,
            MemberNotificationSettingsRepository settingsRepository,
            NotificationCreationService notificationCreationService,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.settingsRepository = settingsRepository;
        this.notificationCreationService = notificationCreationService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    @Transactional
    public void process() {
        processBatch();
    }

    @Transactional
    public void processBatch() {
        MarketingNotificationCampaign campaign = campaignRepository
                .findNextForUpdate(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
        if (campaign == null) {
            return;
        }
        campaign.claim(clock.instant());
        List<Long> memberIds = settingsRepository.findMemberIdsWithMarketingEnabledAfter(
                MemberStatus.ACTIVE,
                campaign.getLastMemberId(),
                PageRequest.of(0, BATCH_SIZE)
        );
        int queuedCount = memberIds.stream()
                .map(memberId -> createNotification(campaign, memberId))
                .mapToInt(queued -> queued ? 1 : 0)
                .sum();
        long lastMemberId = memberIds.isEmpty()
                ? campaign.getLastMemberId()
                : memberIds.getLast();
        campaign.advance(
                lastMemberId,
                queuedCount,
                memberIds.size() == BATCH_SIZE,
                clock.instant()
        );
    }

    private boolean createNotification(
            MarketingNotificationCampaign campaign,
            Long memberId
    ) {
        Instant now = clock.instant();
        return notificationCreationService.createMarketingNotification(new NotificationRequest(
                memberId,
                NotificationType.MARKETING,
                campaign.getTitle(),
                campaign.getBody(),
                NotificationRoute.NOTICE,
                campaign.getId(),
                "MARKETING:" + campaign.getId(),
                now
        ));
    }
}
