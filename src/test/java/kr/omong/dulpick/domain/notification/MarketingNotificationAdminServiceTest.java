package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationAdminService;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationCommand;
import kr.omong.dulpick.domain.notification.application.command.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.command.NotificationRequest;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingNotificationAdminServiceTest {

    private final MemberNotificationSettingsRepository settingsRepository = mock(
            MemberNotificationSettingsRepository.class
    );
    private final NotificationCreationService creationService = mock(NotificationCreationService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-24T00:00:00Z"),
            ZoneOffset.UTC
    );
    private final MarketingNotificationAdminService service = new MarketingNotificationAdminService(
            settingsRepository,
            creationService,
            clock
    );

    @Test
    void queuesMarketingNotificationOnlyForActiveConsentTargets() {
        when(settingsRepository.findMemberIdsWithMarketingEnabled(MemberStatus.ACTIVE))
                .thenReturn(List.of(10L, 20L));
        when(creationService.createMarketingNotification(any(NotificationRequest.class)))
                .thenAnswer(invocation -> ((NotificationRequest) invocation.getArgument(0))
                        .receiverMemberId().equals(10L));

        var result = service.send(new MarketingNotificationCommand("제목", "본문"));

        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.queuedAt()).isEqualTo(Instant.parse("2026-08-24T00:00:00Z"));
        verify(creationService).createMarketingNotification(argThat(request ->
                request.receiverMemberId() == 10L
                        && request.type().name().equals("MARKETING")
                        && request.title().equals("제목")
                        && request.body().equals("본문")
                        && request.deduplicationKey().startsWith("MARKETING:")
        ));
    }
}
