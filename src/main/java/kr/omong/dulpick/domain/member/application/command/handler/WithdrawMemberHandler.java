package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.auth.application.support.AppleAccountRevocationService;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.couple.application.support.CoupleDisconnectionService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class WithdrawMemberHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppleAccountRevocationService appleAccountRevocationService;
    private final CoupleDisconnectionService coupleDisconnectionService;
    private final PushDeviceService pushDeviceService;
    private final Clock clock;

    public WithdrawMemberHandler(
            RefreshTokenRepository refreshTokenRepository,
            AppleAccountRevocationService appleAccountRevocationService,
            CoupleDisconnectionService coupleDisconnectionService,
            PushDeviceService pushDeviceService,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appleAccountRevocationService = appleAccountRevocationService;
        this.coupleDisconnectionService = coupleDisconnectionService;
        this.pushDeviceService = pushDeviceService;
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
        pushDeviceService.disableAllForWithdrawal(memberId, withdrawnAt);
        appleAccountRevocationService.enqueueForMember(memberId);
    }

    private void withdraw(Member member, Long memberId, Instant withdrawnAt) {
        member.withdraw(withdrawnAt);
        refreshTokenRepository.revokeAllByMemberId(memberId, withdrawnAt);
    }
}
