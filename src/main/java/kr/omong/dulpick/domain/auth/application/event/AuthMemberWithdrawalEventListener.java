package kr.omong.dulpick.domain.auth.application.event;

import kr.omong.dulpick.domain.auth.application.support.AppleAccountRevocationService;
import kr.omong.dulpick.domain.member.domain.event.MemberWithdrawnEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuthMemberWithdrawalEventListener {

    private final AppleAccountRevocationService appleAccountRevocationService;

    public AuthMemberWithdrawalEventListener(AppleAccountRevocationService appleAccountRevocationService) {
        this.appleAccountRevocationService = appleAccountRevocationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onWithdrawn(MemberWithdrawnEvent event) {
        appleAccountRevocationService.enqueueForMember(event.memberId());
    }
}
