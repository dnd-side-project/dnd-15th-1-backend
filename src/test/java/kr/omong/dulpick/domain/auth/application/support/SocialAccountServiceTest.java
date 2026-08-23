package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.application.support.model.AuthenticatedMember;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SocialAccountServiceTest {

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Test
    void returnsExistingMemberForSameProviderSubject() {
        AuthenticatedMember firstLogin = socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "provider-subject",
                "member@example.com",
                ProviderAuthorization.none()
        );
        AuthenticatedMember secondLogin = socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "provider-subject",
                "updated@example.com",
                ProviderAuthorization.none()
        );

        assertThat(firstLogin.newMember()).isTrue();
        assertThat(secondLogin.newMember()).isFalse();
        assertThat(secondLogin.member().getId()).isEqualTo(firstLogin.member().getId());
    }

    @Test
    void createsReplacementMemberInsteadOfReactivatingWithdrawnMember() {
        AuthenticatedMember firstLogin = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "withdrawn-subject",
                "member@example.com",
                ProviderAuthorization.none()
        );
        firstLogin.member().withdraw(Instant.parse("2026-08-23T00:00:00Z"));

        AuthenticatedMember secondLogin = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "withdrawn-subject",
                "updated@example.com",
                ProviderAuthorization.none()
        );

        assertThat(secondLogin.newMember()).isTrue();
        assertThat(secondLogin.member().getId()).isNotEqualTo(firstLogin.member().getId());
        assertThat(firstLogin.member().isActive()).isFalse();
        assertThat(socialAccountRepository
                .findByProviderAndProviderSubject(SocialProvider.KAKAO, "withdrawn-subject")
                .orElseThrow()
                .getMember()
                .getId()).isEqualTo(secondLogin.member().getId());
    }

    @Test
    void keepsAppleEmailAndAuthorizationWhenLaterLoginOmitsThem() {
        socialAccountService.getOrCreate(
                SocialProvider.APPLE,
                "apple-subject",
                "first-login@example.com",
                new ProviderAuthorization(
                        "encrypted-refresh-token",
                        "com.dulpick.app"
                )
        );

        socialAccountService.getOrCreate(
                SocialProvider.APPLE,
                "apple-subject",
                null,
                ProviderAuthorization.clientIdOnly("com.dulpick.dev")
        );

        SocialAccount account = socialAccountRepository
                .findByProviderAndProviderSubject(
                        SocialProvider.APPLE,
                        "apple-subject"
                )
                .orElseThrow();
        assertThat(account.getEmail()).isEqualTo("first-login@example.com");
        assertThat(account.getProviderRefreshToken())
                .isEqualTo("encrypted-refresh-token");
        assertThat(account.getProviderClientId()).isEqualTo("com.dulpick.app");
    }

    @Test
    void storesVerifiedAppleClientIdWithoutAuthorizationCode() {
        socialAccountService.getOrCreate(
                SocialProvider.APPLE,
                "apple-dev-subject",
                null,
                ProviderAuthorization.clientIdOnly("com.dulpick.dev")
        );

        SocialAccount account = socialAccountRepository
                .findByProviderAndProviderSubject(
                        SocialProvider.APPLE,
                        "apple-dev-subject"
                )
                .orElseThrow();
        assertThat(account.getProviderRefreshToken()).isNull();
        assertThat(account.getProviderClientId()).isEqualTo("com.dulpick.dev");
    }
}
