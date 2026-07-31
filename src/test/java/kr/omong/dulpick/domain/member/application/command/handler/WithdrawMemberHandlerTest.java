package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.auth.application.AppleAccountRevocationService;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WithdrawMemberHandlerTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final RefreshTokenRepository refreshTokenRepository =
            mock(RefreshTokenRepository.class);
    private final AppleAccountRevocationService appleAccountRevocationService =
            mock(AppleAccountRevocationService.class);
    private final WithdrawMemberHandler handler = new WithdrawMemberHandler(
            memberRepository,
            refreshTokenRepository,
            appleAccountRevocationService,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void enqueuesAppleRevocationAndCompletesLocalWithdrawal() {
        Member member = Member.create();
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));

        handler.handle(1L);

        verify(appleAccountRevocationService).enqueueForMember(1L);
        verify(refreshTokenRepository).revokeAllByMemberId(1L, NOW);
        assertThat(member.isActive()).isFalse();
    }
}
