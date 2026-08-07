package kr.omong.dulpick.domain.couple;

import kr.omong.dulpick.domain.couple.application.exception.ConnectionRateLimitExceededException;
import kr.omong.dulpick.domain.couple.application.exception.InvalidConnectionCodeException;
import kr.omong.dulpick.domain.couple.application.support.ConnectionAbusePreventionService;
import kr.omong.dulpick.domain.couple.application.support.ConnectionAbusePreventionService.AttemptPermit;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttempt;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttemptRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionRateLimitSubjectRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "couple.abuse.preview-per-minute=10",
        "couple.abuse.preview-per-hour=10",
        "couple.abuse.connect-per-minute=2",
        "couple.abuse.connect-per-day=10",
        "couple.abuse.state-changes-per-day=10",
        "couple.abuse.code-failures-per-ten-minutes=2",
        "couple.abuse.ip-failures-per-hour=2"
})
class ConnectionAbusePreventionIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ConnectionAttemptRepository connectionAttemptRepository;

    @Autowired
    private ConnectionRateLimitSubjectRepository subjectRepository;

    @Autowired
    private ConnectionAbusePreventionService abusePreventionService;

    private final List<Long> testMemberIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        testMemberIds.forEach(connectionAttemptRepository::deleteAllByMemberId);
        testMemberIds.forEach(subjectRepository::deleteById);
        testMemberIds.forEach(memberRepository::deleteById);
    }

    @Test
    void blocksMemberWhenConnectionAttemptLimitIsExceeded() {
        Long memberId = createMember();

        completeSuccess(beginAllowed(memberId, "192.0.2.1", ConnectionAttempt.Action.CONNECT));
        completeSuccess(beginAllowed(memberId, "192.0.2.1", ConnectionAttempt.Action.CONNECT));
        AttemptPermit denied = abusePreventionService.begin(
                memberId,
                "192.0.2.1",
                ConnectionAttempt.Action.CONNECT
        );

        assertThat(denied.allowed()).isFalse();
        assertThatThrownBy(denied::requireAllowed)
                .isInstanceOf(ConnectionRateLimitExceededException.class);
    }

    @Test
    void blocksMemberForRepeatedInvalidCodeFailures() {
        Long memberId = createMember();

        completeCodeFailure(memberId, beginAllowed(
                memberId,
                "192.0.2.2",
                ConnectionAttempt.Action.PREVIEW
        ));
        completeCodeFailure(memberId, beginAllowed(
                memberId,
                "192.0.2.2",
                ConnectionAttempt.Action.PREVIEW
        ));

        AttemptPermit denied = abusePreventionService.begin(
                memberId,
                "192.0.2.3",
                ConnectionAttempt.Action.PREVIEW
        );

        assertThat(denied.allowed()).isFalse();
    }

    @Test
    void appliesHashedIpFailureLimitAcrossMembers() {
        Long firstMemberId = createMember();
        Long secondMemberId = createMember();
        Long thirdMemberId = createMember();
        String sharedAddress = "198.51.100.10";
        completeCodeFailure(firstMemberId, beginAllowed(
                firstMemberId,
                sharedAddress,
                ConnectionAttempt.Action.PREVIEW
        ));
        completeCodeFailure(secondMemberId, beginAllowed(
                secondMemberId,
                sharedAddress,
                ConnectionAttempt.Action.PREVIEW
        ));

        AttemptPermit denied = abusePreventionService.begin(
                thirdMemberId,
                sharedAddress,
                ConnectionAttempt.Action.PREVIEW
        );

        assertThat(denied.allowed()).isFalse();
    }

    @Test
    void doesNotShareIpFailureLimitWhenClientAddressIsUnknown() {
        Long firstMemberId = createMember();
        Long secondMemberId = createMember();
        Long thirdMemberId = createMember();
        completeCodeFailure(firstMemberId, beginAllowed(
                firstMemberId,
                null,
                ConnectionAttempt.Action.PREVIEW
        ));
        completeCodeFailure(secondMemberId, beginAllowed(
                secondMemberId,
                " ",
                ConnectionAttempt.Action.PREVIEW
        ));

        AttemptPermit permit = abusePreventionService.begin(
                thirdMemberId,
                null,
                ConnectionAttempt.Action.PREVIEW
        );

        assertThat(permit.allowed()).isTrue();
    }

    private AttemptPermit beginAllowed(
            Long memberId,
            String clientAddress,
            ConnectionAttempt.Action action
    ) {
        AttemptPermit permit = abusePreventionService.begin(memberId, clientAddress, action);
        permit.requireAllowed();
        return permit;
    }

    private void completeSuccess(AttemptPermit permit) {
        abusePreventionService.completeSuccess(permit);
    }

    private void completeCodeFailure(Long memberId, AttemptPermit permit) {
        abusePreventionService.completeFailure(
                memberId,
                permit,
                new InvalidConnectionCodeException()
        );
    }

    private Long createMember() {
        Member member = memberRepository.save(Member.create());
        testMemberIds.add(member.getId());
        return member.getId();
    }
}
