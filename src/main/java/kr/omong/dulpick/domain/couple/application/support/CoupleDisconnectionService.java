package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.CoupleNotFoundException;
import kr.omong.dulpick.domain.couple.application.exception.ConnectionConflictException;
import kr.omong.dulpick.domain.couple.application.exception.CoupleStateInvalidException;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeIssuedReason;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.couple.domain.event.CoupleDisconnectedEvent;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class CoupleDisconnectionService {

    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final CoupleRepository coupleRepository;
    private final MemberRepository memberRepository;
    private final ConnectionCodeRepository connectionCodeRepository;
    private final ConnectionCodeIssuer connectionCodeIssuer;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public CoupleDisconnectionService(
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            CoupleRepository coupleRepository,
            MemberRepository memberRepository,
            ConnectionCodeRepository connectionCodeRepository,
            ConnectionCodeIssuer connectionCodeIssuer,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.coupleRepository = coupleRepository;
        this.memberRepository = memberRepository;
        this.connectionCodeRepository = connectionCodeRepository;
        this.connectionCodeIssuer = connectionCodeIssuer;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void disconnectByMember(Long memberId) {
        Instant disconnectedAt = clock.instant();
        DisconnectionContext context = lockContext(memberId, true);
        validateActiveMembers(context.members());
        disconnect(
                context,
                memberId,
                CoupleDisconnectedEvent.Reason.USER_REQUEST,
                disconnectedAt
        );
        issueCodes(context.members(), null, disconnectedAt);
    }

    @Transactional
    public Member disconnectForWithdrawal(Long memberId, Instant withdrawnAt) {
        DisconnectionContext context = lockContext(memberId, false);
        if (context.couple() == null) {
            revokeActiveCodes(memberId, withdrawnAt);
            return findMember(context.members(), memberId);
        }
        disconnect(
                context,
                memberId,
                CoupleDisconnectedEvent.Reason.MEMBER_WITHDRAWAL,
                withdrawnAt
        );
        issueCodes(context.members(), memberId, withdrawnAt);
        return findMember(context.members(), memberId);
    }

    private DisconnectionContext lockContext(Long memberId, boolean coupleRequired) {
        Optional<ActiveCoupleMember> membership = activeCoupleMemberRepository
                .findByMemberId(memberId);
        if (membership.isEmpty()) {
            return contextWithoutCouple(memberId, coupleRequired);
        }
        Long coupleId = membership.get().getCouple().getId();
        List<Member> members = lockMembers(coupleId, memberId);
        Optional<ActiveCoupleMember> lockedMembership = activeCoupleMemberRepository
                .findForUpdateByMemberId(memberId);
        if (lockedMembership.isEmpty()) {
            return contextWithoutCouple(members, coupleRequired);
        }
        validateUnchangedCouple(coupleId, lockedMembership.get());
        Couple couple = coupleRepository.findForUpdateById(coupleId)
                .orElseThrow(CoupleNotFoundException::new);
        List<ActiveCoupleMember> memberships = activeCoupleMemberRepository
                .findAllForUpdateByCoupleId(coupleId);
        return new DisconnectionContext(couple, members, memberships);
    }

    private DisconnectionContext contextWithoutCouple(
            Long memberId,
            boolean coupleRequired
    ) {
        if (coupleRequired) {
            throw new CoupleNotFoundException();
        }
        return new DisconnectionContext(null, List.of(lockMember(memberId)), List.of());
    }

    private DisconnectionContext contextWithoutCouple(
            List<Member> members,
            boolean coupleRequired
    ) {
        if (coupleRequired) {
            throw new CoupleNotFoundException();
        }
        return new DisconnectionContext(null, members, List.of());
    }

    private List<Member> lockMembers(Long coupleId, Long requesterId) {
        List<Long> memberIds = activeCoupleMemberRepository.findAllByCoupleId(coupleId)
                .stream()
                .map(ActiveCoupleMember::getMemberId)
                .sorted()
                .toList();
        if (memberIds.isEmpty()) {
            return List.of(lockMember(requesterId));
        }
        return memberIds.stream().map(this::lockMember).toList();
    }

    private Member lockMember(Long memberId) {
        return memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void validateActiveMembers(List<Member> members) {
        if (members.stream().anyMatch(member -> !member.isActive())) {
            throw new MemberNotActiveException();
        }
    }

    private void validateUnchangedCouple(
            Long expectedCoupleId,
            ActiveCoupleMember lockedMembership
    ) {
        Long actualCoupleId = lockedMembership.getCouple().getId();
        if (!expectedCoupleId.equals(actualCoupleId)) {
            throw new ConnectionConflictException();
        }
    }

    private void disconnect(
            DisconnectionContext context,
            Long requestedByMemberId,
            CoupleDisconnectedEvent.Reason reason,
            Instant disconnectedAt
    ) {
        validateContext(context, requestedByMemberId);
        Member requestedBy = findMember(context.members(), requestedByMemberId);
        context.couple().disconnect(requestedBy, disconnectedAt);
        activeCoupleMemberRepository.deleteAll(context.memberships());
        activeCoupleMemberRepository.flush();
        publishEvent(context, requestedByMemberId, reason, disconnectedAt);
    }

    private void issueCodes(
            List<Member> members,
            Long excludedMemberId,
            Instant issuedAt
    ) {
        members.forEach(member -> revokeActiveCodes(member.getId(), issuedAt));
        members.stream()
                .filter(Member::isActive)
                .filter(member -> !member.getId().equals(excludedMemberId))
                .forEach(member -> connectionCodeIssuer.issue(
                        member,
                        ConnectionCodeIssuedReason.DISCONNECTION
                ));
    }

    private void revokeActiveCodes(Long memberId, Instant revokedAt) {
        connectionCodeRepository.findAllForUpdateByMemberIdAndStatus(
                memberId,
                ConnectionCodeStatus.ACTIVE
        ).forEach(code -> code.revoke(revokedAt));
    }

    private Member findMember(List<Member> members, Long memberId) {
        return members.stream()
                .filter(member -> member.getId().equals(memberId))
                .findFirst()
                .orElseThrow(MemberNotFoundException::new);
    }

    private void validateContext(
            DisconnectionContext context,
            Long requestedByMemberId
    ) {
        List<Long> memberIds = context.members().stream().map(Member::getId).toList();
        List<Long> membershipIds = context.memberships().stream()
                .map(ActiveCoupleMember::getMemberId)
                .toList();
        boolean sameMembers = memberIds.size() == 2
                && membershipIds.size() == 2
                && memberIds.containsAll(membershipIds)
                && memberIds.contains(requestedByMemberId);
        if (!sameMembers) {
            throw new CoupleStateInvalidException();
        }
    }

    private void publishEvent(
            DisconnectionContext context,
            Long requestedByMemberId,
            CoupleDisconnectedEvent.Reason reason,
            Instant disconnectedAt
    ) {
        List<Long> memberIds = context.members().stream()
                .map(Member::getId)
                .sorted(Comparator.naturalOrder())
                .toList();
        eventPublisher.publishEvent(new CoupleDisconnectedEvent(
                context.couple().getId(),
                memberIds.get(0),
                memberIds.get(1),
                requestedByMemberId,
                reason,
                disconnectedAt
        ));
    }

    private record DisconnectionContext(
            Couple couple,
            List<Member> members,
            List<ActiveCoupleMember> memberships
    ) {
    }
}
