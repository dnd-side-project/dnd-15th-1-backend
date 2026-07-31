package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RefreshTokenConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 2;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Long testMemberId;

    @AfterEach
    @Transactional
    void cleanUp() {
        if (testMemberId == null) {
            return;
        }
        refreshTokenRepository.deleteAll(
                refreshTokenRepository.findAll().stream()
                        .filter(token -> testMemberId.equals(token.getMember().getId()))
                        .toList()
        );
        refreshTokenRepository.flush();
        memberRepository.deleteById(testMemberId);
    }

    @Test
    @Timeout(10)
    void allowsOnlyOneConcurrentRotation() throws Exception {
        Member member = memberRepository.save(Member.create());
        testMemberId = member.getId();
        IssuedTokens issuedTokens = tokenService.issue(member);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Object>> futures = List.of(
                    submitRotation(executor, ready, start, issuedTokens.refreshToken()),
                    submitRotation(executor, ready, start, issuedTokens.refreshToken())
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = futures.stream()
                    .map(this::getResult)
                    .toList();

            assertThat(results).filteredOn(IssuedTokens.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(InvalidRefreshTokenException.class::isInstance)
                    .hasSize(1);
            IssuedTokens successfulRotation = results.stream()
                    .filter(IssuedTokens.class::isInstance)
                    .map(IssuedTokens.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertThatThrownBy(() -> tokenService.rotate(
                    successfulRotation.refreshToken()
            )).isInstanceOf(InvalidRefreshTokenException.class);
        } finally {
            executor.shutdownNow();
        }
    }

    private Future<Object> submitRotation(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            String refreshToken
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                return tokenService.rotate(refreshToken);
            } catch (InvalidRefreshTokenException exception) {
                return exception;
            }
        });
    }

    private Object getResult(Future<Object> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
