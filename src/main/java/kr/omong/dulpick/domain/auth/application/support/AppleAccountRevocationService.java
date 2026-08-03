package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutbox;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AppleAccountRevocationService {

    private final SocialAccountRepository socialAccountRepository;
    private final AppleRevocationOutboxRepository outboxRepository;
    private final Clock clock;

    public AppleAccountRevocationService(
            SocialAccountRepository socialAccountRepository,
            AppleRevocationOutboxRepository outboxRepository,
            Clock clock
    ) {
        this.socialAccountRepository = socialAccountRepository;
        this.outboxRepository = outboxRepository;
        this.clock = clock;
    }

    public void enqueueForMember(Long memberId) {
        socialAccountRepository.findAllByMemberId(memberId).stream()
                .filter(this::hasAppleRefreshToken)
                .forEach(account -> enqueue(memberId, account));
    }

    private boolean hasAppleRefreshToken(SocialAccount account) {
        return account.getProvider() == SocialProvider.APPLE
                && account.getProviderRefreshToken() != null;
    }

    private void enqueue(Long memberId, SocialAccount account) {
        AppleRevocationOutbox outbox = AppleRevocationOutbox.create(
                memberId,
                account.getProviderRefreshToken(),
                account.getProviderClientId(),
                clock.instant()
        );
        outboxRepository.save(outbox);
        account.clearProviderAuthorization();
    }
}
