package kr.omong.dulpick.domain.couple;

import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.couple.domain.CoupleStatus;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoupleConnectionConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 2;
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
    private CoupleRepository coupleRepository;

    @Autowired
    private ActiveCoupleMemberRepository activeCoupleMemberRepository;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private CoupleCommandService coupleCommandService;

    private final List<Long> testMemberIds = new ArrayList<>();
    private final List<Long> testCoupleIds = new ArrayList<>();

    @AfterEach
    @Transactional
    void cleanUp() {
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
        testMemberIds.forEach(memberRepository::deleteById);
    }

    @Test
    @Timeout(15)
    void allowsOnlyOneConcurrentConnectionWithSameInvitationCode() throws Exception {
        ProfileMember inviter = createMember("초대자", 1);
        ProfileMember firstRequester = createMember("요청일", 2);
        ProfileMember secondRequester = createMember("요청이", 3);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Object>> futures = List.of(
                    submitConnection(
                            executor,
                            ready,
                            start,
                            firstRequester.memberId(),
                            inviter.connectionCode()
                    ),
                    submitConnection(
                            executor,
                            ready,
                            start,
                            secondRequester.memberId(),
                            inviter.connectionCode()
                    )
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = futures.stream().map(this::getResult).toList();

            assertThat(results).filteredOn(CoupleConnectionStatus.class::isInstance)
                    .hasSize(1);
            assertThat(results).filteredOn(BusinessException.class::isInstance)
                    .hasSize(1);
            long connectedRequesters = List.of(
                            firstRequester.memberId(),
                            secondRequester.memberId()
                    ).stream()
                    .filter(id -> activeCoupleMemberRepository.findByMemberId(id).isPresent())
                    .count();
            assertThat(connectedRequesters).isEqualTo(1);
            ActiveCoupleMember inviterMembership = activeCoupleMemberRepository
                    .findByMemberId(inviter.memberId())
                    .orElseThrow();
            testCoupleIds.add(inviterMembership.getCouple().getId());
            assertThat(activeCoupleMemberRepository.findAllByCoupleId(
                    inviterMembership.getCouple().getId()
            )).hasSize(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(15)
    void keepsWithdrawalStateConsistentDuringConcurrentDisconnection() throws Exception {
        ProfileMember withdrawing = createMember("탈퇴자", 1);
        ProfileMember partner = createMember("상대방", 2);
        coupleCommandService.connect(
                withdrawing.memberId(),
                new ConnectCoupleCommand(partner.connectionCode())
        );
        Long coupleId = activeCoupleMemberRepository.findByMemberId(withdrawing.memberId())
                .orElseThrow()
                .getCouple()
                .getId();
        testCoupleIds.add(coupleId);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Object>> futures = List.of(
                    submitAction(executor, ready, start, () ->
                            coupleCommandService.disconnect(withdrawing.memberId())),
                    submitAction(executor, ready, start, () ->
                            memberCommandService.withdraw(withdrawing.memberId()))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            futures.forEach(this::getResult);

            assertThat(memberRepository.findById(withdrawing.memberId()).orElseThrow().isActive())
                    .isFalse();
            assertThat(activeCoupleMemberRepository.findByMemberId(withdrawing.memberId()))
                    .isEmpty();
            assertThat(activeCoupleMemberRepository.findByMemberId(partner.memberId()))
                    .isEmpty();
            assertThat(connectionCodeRepository.findByMemberIdAndStatus(
                    withdrawing.memberId(),
                    ConnectionCodeStatus.ACTIVE
            )).isEmpty();
            assertThat(connectionCodeRepository.findAllByMemberId(partner.memberId()))
                    .filteredOn(code -> code.getStatus() == ConnectionCodeStatus.ACTIVE)
                    .hasSize(1);
            assertThat(coupleRepository.findById(coupleId).orElseThrow().getStatus())
                    .isEqualTo(CoupleStatus.DISCONNECTED);
        } finally {
            executor.shutdownNow();
        }
    }

    private Future<Object> submitAction(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            Runnable action
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                action.run();
                return Boolean.TRUE;
            } catch (BusinessException exception) {
                return exception;
            }
        });
    }

    private Future<Object> submitConnection(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            Long requesterId,
            String connectionCode
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                return coupleCommandService.connect(
                        requesterId,
                        new ConnectCoupleCommand(connectionCode)
                );
            } catch (BusinessException exception) {
                return exception;
            }
        });
    }

    private Object getResult(Future<Object> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ProfileMember createMember(String nickname, int profileIcon) {
        Member member = memberRepository.save(Member.create());
        testMemberIds.add(member.getId());
        String code = memberCommandService.initializeProfile(
                member.getId(),
                new InitializeMemberProfileCommand(nickname, profileIcon, PREFERENCES)
        ).connectionCode().code();
        return new ProfileMember(member.getId(), code);
    }

    private record ProfileMember(Long memberId, String connectionCode) {
    }
}
