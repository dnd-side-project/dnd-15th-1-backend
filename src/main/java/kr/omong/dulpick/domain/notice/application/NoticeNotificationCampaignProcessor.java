package kr.omong.dulpick.domain.notice.application;

import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaign;
import kr.omong.dulpick.domain.notice.domain.NoticeNotificationCampaignRepository;
import kr.omong.dulpick.domain.notification.application.command.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.command.NotificationRequest;
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
public class NoticeNotificationCampaignProcessor {

    private static final int BATCH_SIZE = 100;

    private final NoticeNotificationCampaignRepository campaignRepository;
    private final MemberRepository memberRepository;
    private final NotificationCreationService notificationCreationService;
    private final Clock clock;

    public NoticeNotificationCampaignProcessor(
            NoticeNotificationCampaignRepository campaignRepository,
            MemberRepository memberRepository,
            NotificationCreationService notificationCreationService,
            Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.memberRepository = memberRepository;
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
        NoticeNotificationCampaign campaign = campaignRepository
                .findNextForUpdate(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
        if (campaign == null) {
            return;
        }
        campaign.claim(clock.instant());
        List<Long> memberIds = memberRepository.findIdsByStatusAfter(
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

    private boolean createNotification(NoticeNotificationCampaign campaign, Long memberId) {
        Instant now = clock.instant();
        return notificationCreationService.createSystemNotificationIfAbsent(new NotificationRequest(
                memberId,
                NotificationType.ANNOUNCEMENT,
                campaign.getTitle(),
                campaign.getBody(),
                NotificationRoute.NOTICE,
                campaign.getNoticeId().toString(),
                "ANNOUNCEMENT:" + campaign.getNoticeId(),
                now
        ));
    }
}
