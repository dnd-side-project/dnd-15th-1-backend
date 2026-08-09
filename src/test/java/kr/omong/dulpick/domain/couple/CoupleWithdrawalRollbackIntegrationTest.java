package kr.omong.dulpick.domain.couple;

import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.support.ConnectionCodeIssuer;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttemptRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeIssuedReason;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.couple.domain.CoupleStatus;
import kr.omong.dulpick.domain.couple.domain.ConnectionRateLimitSubjectRepository;
import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import kr.omong.dulpick.domain.couple.domain.event.CoupleDisconnectedEvent;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@Import(CoupleWithdrawalRollbackIntegrationTest.EventListenerConfig.class)
class CoupleWithdrawalRollbackIntegrationTest {

    private static final DatePreferences PREFERENCES = new DatePreferences(
            DatePreferenceOption.INDOOR,
            DatePreferenceOption.ACTIVE,
            DatePreferenceOption.NIGHT,
            DatePreferenceOption.FOOD
    );

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private ConnectionCodeRepository connectionCodeRepository;

    @Autowired
    private ConnectionAttemptRepository connectionAttemptRepository;

    @Autowired
    private ConnectionRateLimitSubjectRepository rateLimitSubjectRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private ActiveCoupleMemberRepository activeCoupleMemberRepository;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private CoupleCommandService coupleCommandService;

    @Autowired
    private CommittedCoupleEvents committedCoupleEvents;

    @MockitoSpyBean
    private ConnectionCodeIssuer connectionCodeIssuer;

    private final List<Long> testMemberIds = new ArrayList<>();
    private final List<Long> testCoupleIds = new ArrayList<>();

    @BeforeEach
    void clearCommittedEvents() {
        committedCoupleEvents.clear();
    }

    @AfterEach
    @Transactional
    void cleanUp() {
        reset(connectionCodeIssuer);
        List<Couple> couples = testCoupleIds.stream()
                .map(coupleRepository::findById)
                .flatMap(Optional::stream)
                .toList();
        testMemberIds.forEach(activeCoupleMemberRepository::deleteById);
        activeCoupleMemberRepository.flush();
        testMemberIds.forEach(memberId -> connectionCodeRepository.deleteAll(
                connectionCodeRepository.findAllByMemberId(memberId)
        ));
        connectionCodeRepository.flush();
        testMemberIds.forEach(memberProfileRepository::deleteById);
        memberProfileRepository.flush();
        coupleRepository.deleteAll(couples);
        coupleRepository.flush();
        testMemberIds.forEach(connectionAttemptRepository::deleteAllByMemberId);
        connectionAttemptRepository.flush();
        testMemberIds.forEach(rateLimitSubjectRepository::deleteById);
        rateLimitSubjectRepository.flush();
        testMemberIds.forEach(memberRepository::deleteById);
    }

    @Test
    void rollsBackWithdrawalAndDisconnectionWhenPartnerCodeIssueFails() {
        ProfileMember withdrawing = createMember("탈퇴자", 1);
        ProfileMember partner = createMember("상대방", 2);
        connect(withdrawing, partner);
        committedCoupleEvents.clear();
        doThrow(new IllegalStateException("code issue failure"))
                .when(connectionCodeIssuer)
                .issue(any(Member.class), eq(ConnectionCodeIssuedReason.DISCONNECTION));

        assertThatThrownBy(() -> memberCommandService.withdraw(withdrawing.memberId()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(memberRepository.findById(withdrawing.memberId()).orElseThrow().isActive())
                .isTrue();
        assertThat(activeCoupleMemberRepository.findByMemberId(withdrawing.memberId()))
                .isPresent();
        assertThat(activeCoupleMemberRepository.findByMemberId(partner.memberId()))
                .isPresent();
        assertThat(coupleRepository.findById(testCoupleIds.getLast()))
                .get()
                .extracting(Couple::getStatus)
                .isEqualTo(CoupleStatus.ACTIVE);
        assertThat(committedCoupleEvents.disconnectedEvents()).isEmpty();
    }

    @Test
    void deliversConnectionAndDisconnectionEventsOnlyAfterCommit() {
        ProfileMember first = createMember("첫번째", 1);
        ProfileMember second = createMember("두번째", 2);

        connect(first, second);

        assertThat(committedCoupleEvents.connectedEvents()).hasSize(1);
        assertThat(committedCoupleEvents.disconnectedEvents()).isEmpty();

        coupleCommandService.disconnect(first.memberId());

        assertThat(committedCoupleEvents.disconnectedEvents())
                .singleElement()
                .satisfies(event -> assertThat(event.reason())
                        .isEqualTo(CoupleDisconnectedEvent.Reason.USER_REQUEST));
    }

    private void connect(ProfileMember requester, ProfileMember inviter) {
        coupleCommandService.connect(
                requester.memberId(),
                new ConnectCoupleCommand(inviter.connectionCode())
        );
        Long coupleId = activeCoupleMemberRepository.findByMemberId(requester.memberId())
                .orElseThrow()
                .getCouple()
                .getId();
        testCoupleIds.add(coupleId);
    }

    private ProfileMember createMember(String nickname, int profileIcon) {
        Member member = memberRepository.save(Member.create(Instant.EPOCH));
        testMemberIds.add(member.getId());
        String code = memberCommandService.initializeProfile(
                member.getId(),
                new InitializeMemberProfileCommand(nickname, profileIcon, PREFERENCES)
        ).connectionCode().code();
        return new ProfileMember(member.getId(), code);
    }

    private record ProfileMember(Long memberId, String connectionCode) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EventListenerConfig {

        @Bean
        CommittedCoupleEvents committedCoupleEvents() {
            return new CommittedCoupleEvents();
        }
    }

    static class CommittedCoupleEvents {

        private final List<CoupleConnectedEvent> connectedEvents =
                new CopyOnWriteArrayList<>();
        private final List<CoupleDisconnectedEvent> disconnectedEvents =
                new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onConnected(CoupleConnectedEvent event) {
            connectedEvents.add(event);
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onDisconnected(CoupleDisconnectedEvent event) {
            disconnectedEvents.add(event);
        }

        public List<CoupleDisconnectedEvent> disconnectedEvents() {
            return List.copyOf(disconnectedEvents);
        }

        public List<CoupleConnectedEvent> connectedEvents() {
            return List.copyOf(connectedEvents);
        }

        public void clear() {
            connectedEvents.clear();
            disconnectedEvents.clear();
        }
    }
}
