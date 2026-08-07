package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import kr.omong.dulpick.domain.couple.domain.event.CoupleDisconnectedEvent;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CoupleNotificationEventListener {

    private final NotificationCreationService notificationCreationService;

    public CoupleNotificationEventListener(
            NotificationCreationService notificationCreationService
    ) {
        this.notificationCreationService = notificationCreationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onConnected(CoupleConnectedEvent event) {
        createConnected(event, event.firstMemberId());
        createConnected(event, event.secondMemberId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDisconnected(CoupleDisconnectedEvent event) {
        createDisconnected(event, event.firstMemberId());
        createDisconnected(event, event.secondMemberId());
    }

    private void createConnected(CoupleConnectedEvent event, Long receiverMemberId) {
        notificationCreationService.createSystemNotification(new NotificationRequest(
                receiverMemberId,
                NotificationType.COUPLE_CONNECTED,
                "커플 연결이 완료됐어요",
                "상대방과 연결되었습니다.",
                NotificationRoute.COUPLE_STATUS,
                event.coupleId().toString(),
                "COUPLE_CONNECTED:%d:%d".formatted(
                        event.coupleId(),
                        receiverMemberId
                ),
                event.occurredAt()
        ));
    }

    private void createDisconnected(
            CoupleDisconnectedEvent event,
            Long receiverMemberId
    ) {
        notificationCreationService.createSystemNotification(new NotificationRequest(
                receiverMemberId,
                NotificationType.COUPLE_DISCONNECTED,
                "커플 연결이 해제됐어요",
                "상대방과의 연결이 해제되었습니다.",
                NotificationRoute.COUPLE_STATUS,
                event.coupleId().toString(),
                "COUPLE_DISCONNECTED:%d:%d".formatted(
                        event.coupleId(),
                        receiverMemberId
                ),
                event.occurredAt()
        ));
    }
}
