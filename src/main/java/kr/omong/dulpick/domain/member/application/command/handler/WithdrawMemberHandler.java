package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.auth.application.support.AppleAccountRevocationService;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberAlreadyWithdrawnException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class WithdrawMemberHandler {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppleAccountRevocationService appleAccountRevocationService;
    private final Clock clock;

    public WithdrawMemberHandler(
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            AppleAccountRevocationService appleAccountRevocationService,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.appleAccountRevocationService = appleAccountRevocationService;
        this.clock = clock;
    }

    @Transactional
    public void handle(Long memberId) {
        Member member = findMemberForUpdate(memberId);
        validateActive(member);
        appleAccountRevocationService.enqueueForMember(memberId);
        withdraw(member, memberId);
    }

    private Member findMemberForUpdate(Long memberId) {
        return memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void validateActive(Member member) {
        if (!member.isActive()) {
            throw new MemberAlreadyWithdrawnException();
        }
    }

    private void withdraw(Member member, Long memberId) {
        Instant withdrawnAt = clock.instant();
        member.withdraw(withdrawnAt);
        refreshTokenRepository.revokeAllByMemberId(memberId, withdrawnAt);
    }
}
