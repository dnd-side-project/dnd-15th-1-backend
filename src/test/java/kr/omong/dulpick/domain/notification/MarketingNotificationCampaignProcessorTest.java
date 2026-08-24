package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationCampaignProcessor;
import kr.omong.dulpick.domain.notification.application.command.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.command.NotificationRequest;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaign;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingNotificationCampaignProcessorTest {

    private final MarketingNotificationCampaignRepository campaignRepository = mock(
            MarketingNotificationCampaignRepository.class
    );
    private final MemberNotificationSettingsRepository settingsRepository = mock(
            MemberNotificationSettingsRepository.class
    );
    private final NotificationCreationService creationService = mock(NotificationCreationService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-24T00:00:00Z"),
            ZoneOffset.UTC
    );
    private final MarketingNotificationCampaignProcessor processor =
            new MarketingNotificationCampaignProcessor(
                    campaignRepository,
                    settingsRepository,
                    creationService,
                    clock
            );

    @Test
    void processesCampaignInBatchesAndCompletesWhenLastBatchIsShort() {
        MarketingNotificationCampaign campaign = MarketingNotificationCampaign.create(
                "campaign-id",
                "제목",
                "본문",
                2,
                clock.instant()
        );
        when(campaignRepository.findNextForUpdate(PageRequest.of(0, 1)))
                .thenReturn(List.of(campaign));
        when(settingsRepository.findMemberIdsWithMarketingEnabledAfter(
                MemberStatus.ACTIVE,
                0L,
                PageRequest.of(0, 100)
        )).thenReturn(List.of(10L, 20L));
        when(creationService.createMarketingNotification(any(NotificationRequest.class)))
                .thenReturn(true);

        processor.processBatch();

        assertThat(campaign.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(campaign.getQueuedCount()).isEqualTo(2);
        assertThat(campaign.getLastMemberId()).isEqualTo(20L);
        verify(campaignRepository).findNextForUpdate(PageRequest.of(0, 1));
        verify(creationService, org.mockito.Mockito.times(2))
                .createMarketingNotification(any(NotificationRequest.class));
    }
}
