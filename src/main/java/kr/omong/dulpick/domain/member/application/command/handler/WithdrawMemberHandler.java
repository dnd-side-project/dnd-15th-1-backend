package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.couple.application.support.CoupleDisconnectionService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.event.MemberWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class WithdrawMemberHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CoupleDisconnectionService coupleDisconnectionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public WithdrawMemberHandler(
            RefreshTokenRepository refreshTokenRepository,
            CoupleDisconnectionService coupleDisconnectionService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.coupleDisconnectionService = coupleDisconnectionService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void handle(Long memberId) {
        Instant withdrawnAt = clock.instant();
        Member member = coupleDisconnectionService.disconnectForWithdrawal(
                memberId,
                withdrawnAt
        );
        withdraw(member, memberId, withdrawnAt);
        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId, withdrawnAt));
    }

    private void withdraw(Member member, Long memberId, Instant withdrawnAt) {
        member.withdraw(withdrawnAt);
        refreshTokenRepository.revokeAllByMemberId(memberId, withdrawnAt);
    }
}
