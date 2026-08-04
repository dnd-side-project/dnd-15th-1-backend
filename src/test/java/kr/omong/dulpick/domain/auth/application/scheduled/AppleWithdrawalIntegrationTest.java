package kr.omong.dulpick.domain.auth.application.scheduled;

import kr.omong.dulpick.domain.auth.application.support.AppleAuthorizationService;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutbox;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Transactional
class AppleWithdrawalIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private AppleRevocationOutboxRepository outboxRepository;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private AppleRevocationOutboxWorker outboxWorker;

    @MockitoBean
    private AppleAuthorizationService appleAuthorizationService;

    @Test
    void completesLocalWithdrawalBeforeAsynchronousAppleRevocation() {
        Member member = appleMember();

        memberCommandService.withdraw(member.getId());

        assertThat(memberRepository.findById(member.getId()).orElseThrow().isActive())
                .isFalse();
        SocialAccount account = socialAccountRepository.findAllByMemberId(member.getId())
                .getFirst();
        assertThat(account.getProviderRefreshToken()).isNull();
        assertThat(account.getProviderClientId()).isNull();
        AppleRevocationOutbox outbox = outboxRepository.findAll().getFirst();
        verifyNoInteractions(appleAuthorizationService);

        outboxWorker.process(outbox.getId());

        verify(appleAuthorizationService).revoke(
                "encrypted-refresh-token",
                "com.dulpick.app"
        );
        assertThat(outboxRepository.findById(outbox.getId())).isEmpty();
    }

    @Test
    void keepsWithdrawalCompletedAndSchedulesRetryWhenAppleIsUnavailable() {
        Member member = appleMember();
        memberCommandService.withdraw(member.getId());
        AppleRevocationOutbox outbox = outboxRepository.findAll().getFirst();
        doThrow(new AppleAuthorizationException("Apple is unavailable"))
                .when(appleAuthorizationService)
                .revoke("encrypted-refresh-token", "com.dulpick.app");

        outboxWorker.process(outbox.getId());

        assertThat(memberRepository.findById(member.getId()).orElseThrow().isActive())
                .isFalse();
        AppleRevocationOutbox retry = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(retry.getAttemptCount()).isEqualTo(1);
        assertThat(retry.getNextAttemptAt()).isAfter(retry.getCreatedAt());
    }

    private Member appleMember() {
        Member member = memberRepository.save(Member.create());
        SocialAccount account = SocialAccount.create(
                member,
                SocialProvider.APPLE,
                "apple-subject-" + member.getId(),
                "member@example.com"
        );
        account.updateProviderAuthorization(
                "encrypted-refresh-token",
                "com.dulpick.app"
        );
        socialAccountRepository.save(account);
        return member;
    }
}
