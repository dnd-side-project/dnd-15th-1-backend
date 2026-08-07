package kr.omong.dulpick.domain.couple.application.query.reader;

import kr.omong.dulpick.domain.couple.application.exception.CoupleStateInvalidException;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoupleConnectionReaderTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Instant CONNECTED_AT = Instant.parse("2026-08-06T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-07T15:00:00Z"),
            SEOUL
    );

    private final ActiveCoupleMemberRepository membershipRepository =
            mock(ActiveCoupleMemberRepository.class);
    private final MemberProfileRepository profileRepository =
            mock(MemberProfileRepository.class);
    private final CoupleConnectionReader reader = new CoupleConnectionReader(
            membershipRepository,
            profileRepository,
            CLOCK
    );

    @Test
    void calculatesFirstConnectionDateAsDayOneAcrossDateBoundary() {
        Member me = member(1L);
        Member partner = member(2L);
        Couple couple = Couple.connect(CONNECTED_AT);
        ReflectionTestUtils.setField(couple, "id", 10L);
        ActiveCoupleMember myMembership = ActiveCoupleMember.join(me, couple, CONNECTED_AT);
        ActiveCoupleMember partnerMembership = ActiveCoupleMember.join(
                partner,
                couple,
                CONNECTED_AT
        );
        ReflectionTestUtils.setField(myMembership, "memberId", 1L);
        ReflectionTestUtils.setField(partnerMembership, "memberId", 2L);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile(me, "나", 1)));
        when(profileRepository.findById(2L))
                .thenReturn(Optional.of(profile(partner, "상대", 2)));
        when(membershipRepository.findByMemberId(1L))
                .thenReturn(Optional.of(myMembership));
        when(membershipRepository.findAllByCoupleId(10L))
                .thenReturn(List.of(myMembership, partnerMembership));

        CoupleConnectionStatus status = reader.read(1L);

        assertThat(status.connected()).isTrue();
        assertThat(status.daysTogether()).isEqualTo(2);
        assertThat(status.partner().nickname()).isEqualTo("상대");
    }

    @Test
    void rejectsCoupleWithMissingPartnerMembership() {
        Member me = member(1L);
        Couple couple = Couple.connect(CONNECTED_AT);
        ReflectionTestUtils.setField(couple, "id", 10L);
        ActiveCoupleMember myMembership = ActiveCoupleMember.join(me, couple, CONNECTED_AT);
        ReflectionTestUtils.setField(myMembership, "memberId", 1L);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile(me, "나", 1)));
        when(membershipRepository.findByMemberId(1L))
                .thenReturn(Optional.of(myMembership));
        when(membershipRepository.findAllByCoupleId(10L))
                .thenReturn(List.of(myMembership));

        assertThatThrownBy(() -> reader.read(1L))
                .isInstanceOf(CoupleStateInvalidException.class);
    }

    private Member member(Long id) {
        Member member = Member.create();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberProfile profile(Member member, String nickname, int icon) {
        return MemberProfile.create(
                member,
                nickname,
                icon,
                new DatePreferences(
                        DatePreferenceOption.INDOOR,
                        DatePreferenceOption.ACTIVE,
                        DatePreferenceOption.DAY,
                        DatePreferenceOption.FOOD
                ),
                CONNECTED_AT
        );
    }
}
