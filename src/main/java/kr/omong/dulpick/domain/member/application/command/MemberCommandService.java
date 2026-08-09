package kr.omong.dulpick.domain.member.application.command;

import kr.omong.dulpick.domain.member.application.command.handler.WithdrawMemberHandler;
import kr.omong.dulpick.domain.member.application.command.handler.InitializeMemberProfileHandler;
import kr.omong.dulpick.domain.member.application.command.handler.UpdateDatePreferencesHandler;
import kr.omong.dulpick.domain.member.application.command.handler.UpdateMemberProfileHandler;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import org.springframework.stereotype.Service;

@Service
public class MemberCommandService {

    private final WithdrawMemberHandler withdrawMemberHandler;
    private final InitializeMemberProfileHandler initializeMemberProfileHandler;
    private final UpdateMemberProfileHandler updateMemberProfileHandler;
    private final UpdateDatePreferencesHandler updateDatePreferencesHandler;

    public MemberCommandService(
            WithdrawMemberHandler withdrawMemberHandler,
            InitializeMemberProfileHandler initializeMemberProfileHandler,
            UpdateMemberProfileHandler updateMemberProfileHandler,
            UpdateDatePreferencesHandler updateDatePreferencesHandler
    ) {
        this.withdrawMemberHandler = withdrawMemberHandler;
        this.initializeMemberProfileHandler = initializeMemberProfileHandler;
        this.updateMemberProfileHandler = updateMemberProfileHandler;
        this.updateDatePreferencesHandler = updateDatePreferencesHandler;
    }

    public InitializedMemberProfile initializeProfile(
            Long memberId,
            InitializeMemberProfileCommand command
    ) {
        return initializeMemberProfileHandler.handle(memberId, command);
    }

    public UpdatedMemberProfile updateProfile(
            Long memberId,
            UpdateMemberProfileCommand command
    ) {
        return updateMemberProfileHandler.handle(memberId, command);
    }

    public DatePreferences updateDatePreferences(
            Long memberId,
            DatePreferences preferences
    ) {
        return updateDatePreferencesHandler.handle(memberId, preferences);
    }

    public void withdraw(Long memberId) {
        withdrawMemberHandler.handle(memberId);
    }
}
