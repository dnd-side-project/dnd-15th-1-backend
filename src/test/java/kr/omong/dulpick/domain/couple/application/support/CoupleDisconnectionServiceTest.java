package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.ConnectionConflictException;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CoupleDisconnectionServiceTest {

    private final ActiveCoupleMemberRepository membershipRepository =
            mock(ActiveCoupleMemberRepository.class);
    private final CoupleRepository coupleRepository = mock(CoupleRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ConnectionCodeRepository codeRepository = mock(ConnectionCodeRepository.class);
    private final ConnectionCodeIssuer codeIssuer = mock(ConnectionCodeIssuer.class);
    private final ApplicationEventPublisher eventPublisher =
            mock(ApplicationEventPublisher.class);
    private final CoupleDisconnectionService service = new CoupleDisconnectionService(
            membershipRepository,
            coupleRepository,
            memberRepository,
            codeRepository,
            codeIssuer,
            eventPublisher,
            Clock.systemUTC()
    );

    @Test
    void rejectsWhenMembershipMovesToAnotherCoupleWhileLocking() {
        Couple initialCouple = couple(10L);
        Couple changedCouple = couple(11L);
        ActiveCoupleMember initialMembership = membership(1L, initialCouple);
        ActiveCoupleMember partnerMembership = membership(2L, initialCouple);
        ActiveCoupleMember changedMembership = membership(1L, changedCouple);
        Member requester = member(1L);
        Member partner = member(2L);
        when(membershipRepository.findByMemberId(1L))
                .thenReturn(Optional.of(initialMembership));
        when(membershipRepository.findAllByCoupleId(10L))
                .thenReturn(List.of(initialMembership, partnerMembership));
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(requester));
        when(memberRepository.findForUpdateById(2L)).thenReturn(Optional.of(partner));
        when(membershipRepository.findForUpdateByMemberId(1L))
                .thenReturn(Optional.of(changedMembership));

        assertThatThrownBy(() -> service.disconnectByMember(1L))
                .isInstanceOf(ConnectionConflictException.class);
        verifyNoInteractions(coupleRepository, codeRepository, codeIssuer, eventPublisher);
    }

    private ActiveCoupleMember membership(Long memberId, Couple couple) {
        ActiveCoupleMember membership = mock(ActiveCoupleMember.class);
        when(membership.getMemberId()).thenReturn(memberId);
        when(membership.getCouple()).thenReturn(couple);
        return membership;
    }

    private Couple couple(Long coupleId) {
        Couple couple = mock(Couple.class);
        when(couple.getId()).thenReturn(coupleId);
        return couple;
    }

    private Member member(Long memberId) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        return member;
    }
}
