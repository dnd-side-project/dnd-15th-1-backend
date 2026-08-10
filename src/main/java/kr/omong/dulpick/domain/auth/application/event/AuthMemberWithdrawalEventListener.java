package kr.omong.dulpick.domain.auth.application.event;

import kr.omong.dulpick.domain.auth.application.support.AppleAccountRevocationService;
import kr.omong.dulpick.domain.member.domain.event.MemberWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuthMemberWithdrawalEventListener {

    private final AppleAccountRevocationService appleAccountRevocationService;

    public AuthMemberWithdrawalEventListener(AppleAccountRevocationService appleAccountRevocationService) {
        this.appleAccountRevocationService = appleAccountRevocationService;
    }

    @EventListener
    public void onWithdrawn(MemberWithdrawnEvent event) {
        appleAccountRevocationService.enqueueForMember(event.memberId());
    }
}
