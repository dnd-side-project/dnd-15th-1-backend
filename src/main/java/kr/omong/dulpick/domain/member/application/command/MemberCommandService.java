package kr.omong.dulpick.domain.member.application.command;

import kr.omong.dulpick.domain.member.application.command.handler.WithdrawMemberHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberCommandService {

    private final WithdrawMemberHandler withdrawMemberHandler;

    public MemberCommandService(WithdrawMemberHandler withdrawMemberHandler) {
        this.withdrawMemberHandler = withdrawMemberHandler;
    }

    public void withdraw(Long memberId) {
        withdrawMemberHandler.handle(memberId);
    }
}
