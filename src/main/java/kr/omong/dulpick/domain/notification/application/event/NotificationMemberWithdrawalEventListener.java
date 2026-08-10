package kr.omong.dulpick.domain.notification.application.event;

import kr.omong.dulpick.domain.member.domain.event.MemberWithdrawnEvent;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationMemberWithdrawalEventListener {

    private final PushDeviceService pushDeviceService;

    public NotificationMemberWithdrawalEventListener(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @EventListener
    public void onWithdrawn(MemberWithdrawnEvent event) {
        pushDeviceService.disableAllForWithdrawal(event.memberId(), event.withdrawnAt());
    }
}
