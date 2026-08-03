package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutbox;
import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AppleAccountRevocationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");

    private final SocialAccountRepository socialAccountRepository =
            mock(SocialAccountRepository.class);
    private final AppleRevocationOutboxRepository outboxRepository =
            mock(AppleRevocationOutboxRepository.class);
    private final AppleAccountRevocationService service =
            new AppleAccountRevocationService(
                    socialAccountRepository,
                    outboxRepository,
                    Clock.fixed(NOW, ZoneOffset.UTC)
            );

    @Test
    void enqueuesProdAndDevAppleAccountsAndClearsStoredAuthorization() {
        SocialAccount prodAccount = account(
                SocialProvider.APPLE,
                "prod-token",
                "com.dulpick.app"
        );
        SocialAccount devAccount = account(
                SocialProvider.APPLE,
                "dev-token",
                "com.dulpick.dev"
        );
        SocialAccount kakaoAccount = account(SocialProvider.KAKAO, null, null);
        when(socialAccountRepository.findAllByMemberId(1L))
                .thenReturn(List.of(prodAccount, devAccount, kakaoAccount));

        service.enqueueForMember(1L);

        var captor = forClass(AppleRevocationOutbox.class);
        verify(outboxRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(
                        AppleRevocationOutbox::getMemberId,
                        AppleRevocationOutbox::getEncryptedRefreshToken,
                        AppleRevocationOutbox::getClientId
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                1L,
                                "prod-token",
                                "com.dulpick.app"
                        ),
                        tuple(
                                1L,
                                "dev-token",
                                "com.dulpick.dev"
                        )
                );
        assertThat(prodAccount.getProviderRefreshToken()).isNull();
        assertThat(prodAccount.getProviderClientId()).isNull();
        assertThat(devAccount.getProviderRefreshToken()).isNull();
        assertThat(devAccount.getProviderClientId()).isNull();
    }

    @Test
    void skipsAppleAccountWithoutRevocableToken() {
        SocialAccount appleAccount = account(SocialProvider.APPLE, null, null);
        when(socialAccountRepository.findAllByMemberId(1L))
                .thenReturn(List.of(appleAccount));

        service.enqueueForMember(1L);

        verifyNoMoreInteractions(outboxRepository);
    }

    private SocialAccount account(
            SocialProvider provider,
            String encryptedRefreshToken,
            String clientId
    ) {
        SocialAccount account = SocialAccount.create(
                Member.create(),
                provider,
                provider.name().toLowerCase() + "-subject",
                "member@example.com"
        );
        if (encryptedRefreshToken != null) {
            account.updateProviderAuthorization(encryptedRefreshToken, clientId);
        }
        return account;
    }
}
