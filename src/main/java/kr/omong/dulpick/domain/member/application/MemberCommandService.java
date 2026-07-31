package kr.omong.dulpick.domain.member.application;

import kr.omong.dulpick.domain.auth.application.AppleAccountRevocationService;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class MemberCommandService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AppleAccountRevocationService appleAccountRevocationService;
    private final Clock clock;

    public MemberCommandService(
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

    public void withdraw(Long memberId) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberAlreadyWithdrawnException();
        }
        appleAccountRevocationService.enqueueForMember(memberId);
        Instant withdrawnAt = clock.instant();
        member.withdraw(withdrawnAt);
        refreshTokenRepository.revokeAllByMemberId(memberId, withdrawnAt);
    }
}
