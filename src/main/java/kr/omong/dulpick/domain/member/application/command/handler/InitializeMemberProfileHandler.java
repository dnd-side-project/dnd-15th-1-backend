package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.couple.application.support.ConnectionCodeIssuer;
import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeIssuedReason;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.InitializedMemberProfile;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileAlreadyInitializedException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class InitializeMemberProfileHandler {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ConnectionCodeIssuer connectionCodeIssuer;
    private final Clock clock;

    public InitializeMemberProfileHandler(
            MemberRepository memberRepository,
            MemberProfileRepository memberProfileRepository,
            ConnectionCodeIssuer connectionCodeIssuer,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.connectionCodeIssuer = connectionCodeIssuer;
        this.clock = clock;
    }

    @Transactional
    public InitializedMemberProfile handle(
            Long memberId,
            InitializeMemberProfileCommand command
    ) {
        Member member = findActiveMemberForUpdate(memberId);
        validateNotInitialized(memberId);
        Instant now = clock.instant();
        MemberProfile profile = MemberProfile.create(
                member,
                command.nickname(),
                command.profileIcon(),
                command.datePreferences(),
                now
        );
        memberProfileRepository.save(profile);
        IssuedConnectionCode code = connectionCodeIssuer.issue(
                member,
                ConnectionCodeIssuedReason.ONBOARDING
        );
        return toResult(profile, code);
    }

    private Member findActiveMemberForUpdate(Long memberId) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private void validateNotInitialized(Long memberId) {
        if (memberProfileRepository.existsById(memberId)) {
            throw new MemberProfileAlreadyInitializedException();
        }
    }

    private InitializedMemberProfile toResult(
            MemberProfile profile,
            IssuedConnectionCode code
    ) {
        return new InitializedMemberProfile(
                profile.getNickname(),
                profile.getProfileIcon(),
                profile.getDatePreferences(),
                code
        );
    }
}
