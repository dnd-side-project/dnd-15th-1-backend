package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.auth.application.support.AppleAccountRevocationService;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.couple.application.support.CoupleDisconnectionService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.exception.MemberAlreadyWithdrawnException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WithdrawMemberHandlerTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    private final RefreshTokenRepository refreshTokenRepository =
            mock(RefreshTokenRepository.class);
    private final AppleAccountRevocationService appleAccountRevocationService =
            mock(AppleAccountRevocationService.class);
    private final CoupleDisconnectionService coupleDisconnectionService =
            mock(CoupleDisconnectionService.class);
    private final WithdrawMemberHandler handler = new WithdrawMemberHandler(
            refreshTokenRepository,
            appleAccountRevocationService,
            coupleDisconnectionService,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void enqueuesAppleRevocationAndCompletesLocalWithdrawal() {
        Member member = Member.create();
        when(coupleDisconnectionService.disconnectForWithdrawal(1L, NOW))
                .thenReturn(member);

        handler.handle(1L);

        verify(appleAccountRevocationService).enqueueForMember(1L);
        verify(coupleDisconnectionService).disconnectForWithdrawal(1L, NOW);
        verify(refreshTokenRepository).revokeAllByMemberId(1L, NOW);
        assertThat(member.isActive()).isFalse();
        assertThat(member.getTokenVersion()).isEqualTo(1);
        assertThat(member.getLastWithdrawnAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsDuplicateWithdrawalBeforeAuthenticationRevocation() {
        Member member = Member.create();
        member.withdraw(NOW.minusSeconds(1));
        when(coupleDisconnectionService.disconnectForWithdrawal(1L, NOW))
                .thenReturn(member);

        assertThatThrownBy(() -> handler.handle(1L))
                .isInstanceOf(MemberAlreadyWithdrawnException.class);

        verifyNoInteractions(appleAccountRevocationService, refreshTokenRepository);
    }
}
