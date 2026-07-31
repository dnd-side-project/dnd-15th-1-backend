package kr.omong.dulpick.domain.member.application;

import kr.omong.dulpick.domain.auth.application.AppleAccountRevocationService;
import kr.omong.dulpick.domain.auth.application.AppleTokenRevocationException;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MemberCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final RefreshTokenRepository refreshTokenRepository =
            mock(RefreshTokenRepository.class);
    private final AppleAccountRevocationService appleAccountRevocationService =
            mock(AppleAccountRevocationService.class);
    private final MemberCommandService service = new MemberCommandService(
            memberRepository,
            refreshTokenRepository,
            appleAccountRevocationService,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void revokesAppleAuthorizationBeforeLocalWithdrawal() {
        Member member = Member.create();
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));

        service.withdraw(1L);

        verify(appleAccountRevocationService).revokeForMember(1L);
        verify(refreshTokenRepository).revokeAllByMemberId(1L, NOW);
        assertThat(member.isActive()).isFalse();
    }

    @Test
    void doesNotCompleteWithdrawalWhenAppleRevocationFails() {
        Member member = Member.create();
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));
        doThrow(new AppleTokenRevocationException(new RuntimeException()))
                .when(appleAccountRevocationService)
                .revokeForMember(1L);

        assertThatThrownBy(() -> service.withdraw(1L))
                .isInstanceOf(AppleTokenRevocationException.class);

        assertThat(member.isActive()).isTrue();
        verifyNoInteractions(refreshTokenRepository);
    }
}
