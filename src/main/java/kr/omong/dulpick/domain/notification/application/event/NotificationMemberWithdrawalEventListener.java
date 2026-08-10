package kr.omong.dulpick.domain.notification.application.event;

import kr.omong.dulpick.domain.member.domain.event.MemberWithdrawnEvent;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationMemberWithdrawalEventListener {

    private final PushDeviceService pushDeviceService;

    public NotificationMemberWithdrawalEventListener(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onWithdrawn(MemberWithdrawnEvent event) {
        pushDeviceService.disableAllForWithdrawal(event.memberId(), event.withdrawnAt());
    }
}
