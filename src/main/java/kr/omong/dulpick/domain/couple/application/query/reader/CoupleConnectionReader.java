package kr.omong.dulpick.domain.couple.application.query.reader;

import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleMemberProfile;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.global.time.ServiceTime;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class CoupleConnectionReader {

    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final Clock clock;

    public CoupleConnectionReader(
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            MemberProfileRepository memberProfileRepository,
            Clock clock
    ) {
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.clock = clock;
    }

    public CoupleConnectionStatus read(Long memberId) {
        CoupleMemberProfile me = readProfile(memberId);
        return activeCoupleMemberRepository.findByMemberId(memberId)
                .map(membership -> readConnected(memberId, me, membership))
                .orElseGet(() -> CoupleConnectionStatus.disconnected(me));
    }

    private CoupleConnectionStatus readConnected(
            Long memberId,
            CoupleMemberProfile me,
            ActiveCoupleMember membership
    ) {
        List<ActiveCoupleMember> members = activeCoupleMemberRepository.findAllByCoupleId(
                membership.getCouple().getId()
        );
        Long partnerId = members.stream()
                .map(ActiveCoupleMember::getMemberId)
                .filter(id -> !id.equals(memberId))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        Instant connectedAt = membership.getCouple().getConnectedAt();
        return CoupleConnectionStatus.connected(
                me,
                readProfile(partnerId),
                connectedAt,
                calculateDaysTogether(connectedAt)
        );
    }

    private CoupleMemberProfile readProfile(Long memberId) {
        MemberProfile profile = memberProfileRepository.findById(memberId)
                .orElseThrow(MemberProfileRequiredException::new);
        return new CoupleMemberProfile(profile.getNickname(), profile.getProfileIcon());
    }

    private long calculateDaysTogether(Instant connectedAt) {
        LocalDate connectedDate = LocalDate.ofInstant(connectedAt, ServiceTime.ZONE_ID);
        LocalDate currentDate = LocalDate.now(clock.withZone(ServiceTime.ZONE_ID));
        return ChronoUnit.DAYS.between(connectedDate, currentDate) + 1;
    }
}
