package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationAdminService;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationCommand;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaign;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingNotificationAdminServiceTest {

    private final MemberNotificationSettingsRepository settingsRepository = mock(
            MemberNotificationSettingsRepository.class
    );
    private final MarketingNotificationCampaignRepository campaignRepository = mock(
            MarketingNotificationCampaignRepository.class
    );
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-24T00:00:00Z"),
            ZoneOffset.UTC
    );
    private final MarketingNotificationAdminService service = new MarketingNotificationAdminService(
            settingsRepository,
            campaignRepository,
            clock
    );

    @Test
    void createsPendingCampaignWithConsentTargetEstimate() {
        when(settingsRepository.countMembersWithMarketingEnabled(MemberStatus.ACTIVE))
                .thenReturn(42L);
        when(campaignRepository.save(any(MarketingNotificationCampaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.send(new MarketingNotificationCommand("제목", "본문"));

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.targetCount()).isEqualTo(42);
        assertThat(result.queuedCount()).isZero();
        assertThat(result.queuedAt()).isEqualTo(Instant.parse("2026-08-24T00:00:00Z"));
    }
}
