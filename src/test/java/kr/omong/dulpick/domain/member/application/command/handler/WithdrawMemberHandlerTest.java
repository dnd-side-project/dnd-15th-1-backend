package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.couple.application.support.CoupleDisconnectionService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.exception.MemberAlreadyWithdrawnException;
import kr.omong.dulpick.domain.member.domain.event.MemberWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
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
    private final CoupleDisconnectionService coupleDisconnectionService =
            mock(CoupleDisconnectionService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final WithdrawMemberHandler handler = new WithdrawMemberHandler(
            refreshTokenRepository,
            coupleDisconnectionService,
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void enqueuesAppleRevocationAndCompletesLocalWithdrawal() {
        Member member = Member.create(Instant.EPOCH);
        when(coupleDisconnectionService.disconnectForWithdrawal(1L, NOW))
                .thenReturn(member);

        handler.handle(1L);

        verify(eventPublisher).publishEvent(new MemberWithdrawnEvent(1L, NOW));
        verify(coupleDisconnectionService).disconnectForWithdrawal(1L, NOW);
        verify(refreshTokenRepository).revokeAllByMemberId(1L, NOW);
        assertThat(member.isActive()).isFalse();
        assertThat(member.getTokenVersion()).isEqualTo(1);
        assertThat(member.getLastWithdrawnAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsDuplicateWithdrawalBeforeAuthenticationRevocation() {
        Member member = Member.create(Instant.EPOCH);
        member.withdraw(NOW.minusSeconds(1));
        when(coupleDisconnectionService.disconnectForWithdrawal(1L, NOW))
                .thenReturn(member);

        assertThatThrownBy(() -> handler.handle(1L))
                .isInstanceOf(MemberAlreadyWithdrawnException.class);

        verifyNoInteractions(
                refreshTokenRepository,
                eventPublisher
        );
    }
}
