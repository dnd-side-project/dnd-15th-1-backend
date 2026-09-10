package kr.omong.dulpick.domain.analytics.application;

import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnalyticsEventListener {

    private final AnalyticsEventService eventService;

    public AnalyticsEventListener(AnalyticsEventService eventService) {
        this.eventService = eventService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void record(AnalyticsActionEvent event) {
        eventService.record(event);
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void record(CoupleConnectedEvent event) {
        recordMemberConnection(event, event.firstMemberId());
        recordMemberConnection(event, event.secondMemberId());
    }

    private void recordMemberConnection(CoupleConnectedEvent event, Long memberId) {
        if (memberId == null) {
            return;
        }
        record(new AnalyticsActionEvent(
                "COUPLE_CONNECTED:%d:%d".formatted(event.coupleId(), memberId),
                kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType.COUPLE_CONNECTED,
                memberId,
                event.coupleId(),
                "COUPLE",
                event.coupleId(),
                event.occurredAt()
        ));
    }
}
