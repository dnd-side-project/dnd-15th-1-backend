package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.couple.domain.event.CoupleDisconnectedEvent;
import kr.omong.dulpick.domain.notification.application.CoupleNotificationEventListener;
import kr.omong.dulpick.domain.notification.application.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.NotificationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class CoupleNotificationEventListenerTest {

    private final NotificationCreationService creationService =
            mock(NotificationCreationService.class);
    private final CoupleNotificationEventListener listener =
            new CoupleNotificationEventListener(creationService);

    @Test
    void notifiesOnlyPartnerWhenMemberRequestsDisconnection() {
        listener.onDisconnected(event(
                10L,
                CoupleDisconnectedEvent.Reason.USER_REQUEST
        ));

        assertReceiver(20L);
    }

    @Test
    void notifiesOnlyRemainingPartnerWhenMemberWithdraws() {
        listener.onDisconnected(event(
                20L,
                CoupleDisconnectedEvent.Reason.MEMBER_WITHDRAWAL
        ));

        assertReceiver(10L);
    }

    private CoupleDisconnectedEvent event(
            Long requestedByMemberId,
            CoupleDisconnectedEvent.Reason reason
    ) {
        return new CoupleDisconnectedEvent(
                1L,
                10L,
                20L,
                requestedByMemberId,
                reason,
                Instant.parse("2026-08-07T10:00:00Z")
        );
    }

    private void assertReceiver(Long receiverMemberId) {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(
                NotificationRequest.class
        );
        verify(creationService).createSystemNotification(captor.capture());
        verifyNoMoreInteractions(creationService);
        assertThat(captor.getValue().receiverMemberId()).isEqualTo(receiverMemberId);
    }
}
