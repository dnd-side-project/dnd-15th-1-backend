package kr.omong.dulpick.domain.couple.application.command.handler;

import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.exception.ConnectionConflictException;
import kr.omong.dulpick.domain.couple.application.exception.InvalidConnectionCodeException;
import kr.omong.dulpick.domain.couple.application.exception.MemberAlreadyConnectedException;
import kr.omong.dulpick.domain.couple.application.exception.SelfConnectionNotAllowedException;
import kr.omong.dulpick.domain.couple.application.query.reader.CoupleConnectionReader;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.application.support.ConnectionCodeNormalizer;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class ConnectCoupleHandler {

    private final ConnectionCodeRepository connectionCodeRepository;
    private final ConnectionCodeNormalizer connectionCodeNormalizer;
    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final CoupleRepository coupleRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final CoupleConnectionReader coupleConnectionReader;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ConnectCoupleHandler(
            ConnectionCodeRepository connectionCodeRepository,
            ConnectionCodeNormalizer connectionCodeNormalizer,
            MemberRepository memberRepository,
            MemberProfileRepository memberProfileRepository,
            CoupleRepository coupleRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            CoupleConnectionReader coupleConnectionReader,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.connectionCodeRepository = connectionCodeRepository;
        this.connectionCodeNormalizer = connectionCodeNormalizer;
        this.memberRepository = memberRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.coupleRepository = coupleRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.coupleConnectionReader = coupleConnectionReader;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public CoupleConnectionStatus handle(Long requesterId, ConnectCoupleCommand command) {
        ConnectionCode connectionCode = findActiveCodeForUpdate(command.connectionCode());
        Long inviterId = connectionCode.getMember().getId();
        validateNotSelf(requesterId, inviterId);
        List<Member> members = lockActiveMembers(requesterId, inviterId);
        validateProfiles(requesterId, inviterId);
        validateNotConnected(requesterId, inviterId);
        Instant connectedAt = clock.instant();
        Couple couple = createCouple(members, connectedAt);
        consumeCodes(connectionCode, requesterId, connectedAt);
        publishConnected(couple, requesterId, inviterId, connectedAt);
        return coupleConnectionReader.read(requesterId);
    }

    private ConnectionCode findActiveCodeForUpdate(String rawCode) {
        String code = connectionCodeNormalizer.normalize(rawCode);
        ConnectionCode connectionCode = connectionCodeRepository
                .findForUpdateByCodeDigest(Sha256.hex(code))
                .orElseThrow(InvalidConnectionCodeException::new);
        if (connectionCode.getStatus() != ConnectionCodeStatus.ACTIVE) {
            throw new InvalidConnectionCodeException();
        }
        return connectionCode;
    }

    private void validateNotSelf(Long requesterId, Long inviterId) {
        if (requesterId.equals(inviterId)) {
            throw new SelfConnectionNotAllowedException();
        }
    }

    private List<Member> lockActiveMembers(Long requesterId, Long inviterId) {
        List<Long> memberIds = List.of(requesterId, inviterId).stream()
                .sorted()
                .toList();
        List<Member> members = memberIds.stream()
                .map(this::findMemberForUpdate)
                .toList();
        if (members.stream().anyMatch(member -> !member.isActive())) {
            throw new MemberNotActiveException();
        }
        return members;
    }

    private Member findMemberForUpdate(Long memberId) {
        return memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void validateProfiles(Long requesterId, Long inviterId) {
        if (!memberProfileRepository.existsById(requesterId)
                || !memberProfileRepository.existsById(inviterId)) {
            throw new MemberProfileRequiredException();
        }
    }

    private void validateNotConnected(Long requesterId, Long inviterId) {
        if (activeCoupleMemberRepository.findForUpdateByMemberId(requesterId).isPresent()
                || activeCoupleMemberRepository.findForUpdateByMemberId(inviterId).isPresent()) {
            throw new MemberAlreadyConnectedException();
        }
    }

    private Couple createCouple(List<Member> members, Instant connectedAt) {
        Couple couple = coupleRepository.save(Couple.connect(connectedAt));
        List<ActiveCoupleMember> memberships = members.stream()
                .sorted(Comparator.comparing(Member::getId))
                .map(member -> ActiveCoupleMember.join(member, couple, connectedAt))
                .toList();
        try {
            activeCoupleMemberRepository.saveAll(memberships);
            activeCoupleMemberRepository.flush();
            return couple;
        } catch (DataIntegrityViolationException exception) {
            throw new ConnectionConflictException();
        }
    }

    private void consumeCodes(
            ConnectionCode invitationCode,
            Long requesterId,
            Instant connectedAt
    ) {
        invitationCode.use(connectedAt);
        connectionCodeRepository.findAllForUpdateByMemberIdAndStatus(
                requesterId,
                ConnectionCodeStatus.ACTIVE
        ).forEach(code -> code.revoke(connectedAt));
    }

    private void publishConnected(
            Couple couple,
            Long requesterId,
            Long inviterId,
            Instant connectedAt
    ) {
        eventPublisher.publishEvent(new CoupleConnectedEvent(
                couple.getId(),
                requesterId,
                inviterId,
                connectedAt
        ));
    }
}
