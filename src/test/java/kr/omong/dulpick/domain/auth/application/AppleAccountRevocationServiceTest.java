package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AppleAccountRevocationServiceTest {

    private final SocialAccountRepository socialAccountRepository =
            mock(SocialAccountRepository.class);
    private final AppleAuthorizationService appleAuthorizationService =
            mock(AppleAuthorizationService.class);
    private final AppleAccountRevocationService service =
            new AppleAccountRevocationService(
                    socialAccountRepository,
                    appleAuthorizationService
            );

    @Test
    void revokesProdAndDevAppleAccountsWithStoredClientIds() {
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

        service.revokeForMember(1L);

        verify(appleAuthorizationService).revoke(
                "prod-token",
                "com.dulpick.app"
        );
        verify(appleAuthorizationService).revoke(
                "dev-token",
                "com.dulpick.dev"
        );
        verifyNoMoreInteractions(appleAuthorizationService);
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

        service.revokeForMember(1L);

        verifyNoMoreInteractions(appleAuthorizationService);
    }

    @Test
    void preservesAuthorizationAndReturnsRetryableFailureWhenRevocationFails() {
        SocialAccount appleAccount = account(
                SocialProvider.APPLE,
                "encrypted-refresh-token",
                "com.dulpick.app"
        );
        when(socialAccountRepository.findAllByMemberId(1L))
                .thenReturn(List.of(appleAccount));
        doThrow(new AppleAuthorizationException("sensitive-refresh-token"))
                .when(appleAuthorizationService)
                .revoke("encrypted-refresh-token", "com.dulpick.app");

        assertThatThrownBy(() -> service.revokeForMember(1L))
                .isInstanceOf(AppleTokenRevocationException.class)
                .hasMessageNotContaining("encrypted-refresh-token")
                .hasMessageNotContaining("sensitive-refresh-token");
        assertThat(appleAccount.getProviderRefreshToken())
                .isEqualTo("encrypted-refresh-token");
        assertThat(appleAccount.getProviderClientId())
                .isEqualTo("com.dulpick.app");
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
