package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.application.command.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;
import kr.omong.dulpick.domain.auth.application.exception.ConcurrentSocialAccountCreationException;
import kr.omong.dulpick.domain.auth.application.exception.InvalidSocialLoginRequestException;
import kr.omong.dulpick.domain.auth.application.support.AppleAuthorizationService;
import kr.omong.dulpick.domain.auth.application.support.LoginNonceService;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.SocialIdentityVerifierRegistry;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.AuthenticatedMember;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SocialLoginHandlerTest {

    private final SocialIdentityVerifierRegistry verifierRegistry =
            mock(SocialIdentityVerifierRegistry.class);
    private final LoginNonceService loginNonceService = mock(LoginNonceService.class);
    private final AppleAuthorizationService appleAuthorizationService =
            mock(AppleAuthorizationService.class);
    private final SocialAccountService socialAccountService = mock(SocialAccountService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final SocialLoginHandler handler = new SocialLoginHandler(
            verifierRegistry,
            loginNonceService,
            appleAuthorizationService,
            socialAccountService,
            tokenService
    );

    @Test
    void verifiesGoogleNonce() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "id-token",
                null,
                "login-nonce"
        );
        SocialIdentity identity = new SocialIdentity(
                "subject",
                "member@example.com",
                "login-nonce",
                "google-client-id"
        );
        Member member = member(1L);
        when(verifierRegistry.verify(SocialProvider.GOOGLE, "id-token")).thenReturn(identity);
        when(socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "subject",
                "member@example.com",
                ProviderAuthorization.none()
        )).thenReturn(new AuthenticatedMember(member, true));
        when(tokenService.issue(member)).thenReturn(tokens());

        SocialLoginResult result = handler.handle(command);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.newMember()).isTrue();
        verify(loginNonceService).consume(
                SocialProvider.GOOGLE,
                "login-nonce",
                "login-nonce"
        );
    }

    @Test
    void rejectsGoogleLoginWithoutNonceBeforeVerifyingToken() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "id-token",
                null,
                null
        );

        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(InvalidSocialLoginRequestException.class);
        verifyNoInteractions(verifierRegistry, loginNonceService);
    }

    @Test
    void verifiesAppleNonceAndAuthorizationCode() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.APPLE,
                "id-token",
                "authorization-code",
                "raw-nonce"
        );
        SocialIdentity identity = new SocialIdentity(
                "apple-subject",
                "member@example.com",
                "hashed-nonce",
                "com.dulpick.app"
        );
        Member member = member(2L);
        when(verifierRegistry.verify(SocialProvider.APPLE, "id-token")).thenReturn(identity);
        when(appleAuthorizationService.exchange("authorization-code", identity))
                .thenReturn(new ProviderAuthorization(
                        "encrypted-provider-token",
                        "com.dulpick.app"
                ));
        when(socialAccountService.getOrCreate(
                SocialProvider.APPLE,
                "apple-subject",
                "member@example.com",
                new ProviderAuthorization(
                        "encrypted-provider-token",
                        "com.dulpick.app"
                )
        )).thenReturn(new AuthenticatedMember(member, false));
        when(tokenService.issue(member)).thenReturn(tokens());

        handler.handle(command);

        verify(loginNonceService).consume(
                SocialProvider.APPLE,
                "raw-nonce",
                "hashed-nonce"
        );
        verify(appleAuthorizationService).exchange("authorization-code", identity);
    }

    @Test
    void allowsAppleLoginWithoutAuthorizationCode() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.APPLE,
                "id-token",
                null,
                "raw-nonce"
        );
        SocialIdentity identity = new SocialIdentity(
                "apple-subject",
                null,
                "hashed-nonce",
                "com.dulpick.dev"
        );
        Member member = member(3L);
        when(verifierRegistry.verify(SocialProvider.APPLE, "id-token")).thenReturn(identity);
        when(socialAccountService.getOrCreate(
                SocialProvider.APPLE,
                "apple-subject",
                null,
                ProviderAuthorization.clientIdOnly("com.dulpick.dev")
        )).thenReturn(new AuthenticatedMember(member, false));
        when(tokenService.issue(member)).thenReturn(tokens());

        handler.handle(command);

        verify(loginNonceService).consume(
                SocialProvider.APPLE,
                "raw-nonce",
                "hashed-nonce"
        );
        verifyNoInteractions(appleAuthorizationService);
    }

    @Test
    void verifiesKakaoNonce() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.KAKAO,
                "id-token",
                null,
                "login-nonce"
        );
        SocialIdentity identity = new SocialIdentity(
                "kakao-subject",
                "member@example.com",
                "login-nonce",
                "kakao-client-id"
        );
        Member member = member(4L);
        when(verifierRegistry.verify(SocialProvider.KAKAO, "id-token"))
                .thenReturn(identity);
        when(socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "kakao-subject",
                "member@example.com",
                ProviderAuthorization.none()
        )).thenReturn(new AuthenticatedMember(member, false));
        when(tokenService.issue(member)).thenReturn(tokens());

        handler.handle(command);

        verify(loginNonceService).consume(
                SocialProvider.KAKAO,
                "login-nonce",
                "login-nonce"
        );
        verifyNoInteractions(appleAuthorizationService);
    }

    @Test
    void recoversOnlyConcurrentProviderSubjectCreation() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "id-token",
                null,
                "login-nonce"
        );
        SocialIdentity identity = new SocialIdentity(
                "subject",
                "member@example.com",
                "login-nonce",
                "google-client-id"
        );
        Member member = member(5L);
        when(verifierRegistry.verify(SocialProvider.GOOGLE, "id-token")).thenReturn(identity);
        when(socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "subject",
                "member@example.com",
                ProviderAuthorization.none()
        )).thenThrow(new ConcurrentSocialAccountCreationException(
                new DataIntegrityViolationException("provider subject conflict")
        ));
        when(socialAccountService.getExisting(SocialProvider.GOOGLE, "subject"))
                .thenReturn(new AuthenticatedMember(member, false));
        when(tokenService.issue(member)).thenReturn(tokens());

        SocialLoginResult result = handler.handle(command);

        assertThat(result.memberId()).isEqualTo(5L);
        assertThat(result.newMember()).isFalse();
    }

    @Test
    void propagatesUnexpectedIntegrityViolation() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "id-token",
                null,
                "login-nonce"
        );
        SocialIdentity identity = new SocialIdentity(
                "subject",
                "member@example.com",
                "login-nonce",
                "google-client-id"
        );
        DataIntegrityViolationException unexpected =
                new DataIntegrityViolationException("foreign key violation");
        when(verifierRegistry.verify(SocialProvider.GOOGLE, "id-token")).thenReturn(identity);
        when(socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "subject",
                "member@example.com",
                ProviderAuthorization.none()
        )).thenThrow(unexpected);

        assertThatThrownBy(() -> handler.handle(command)).isSameAs(unexpected);
        verifyNoInteractions(tokenService);
    }

    private Member member(Long id) {
        Member member = Member.create();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private IssuedTokens tokens() {
        return new IssuedTokens("access-token", "refresh-token", 900);
    }
}
