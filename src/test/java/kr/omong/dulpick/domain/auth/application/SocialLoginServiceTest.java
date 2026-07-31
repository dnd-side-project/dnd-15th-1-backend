package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SocialLoginServiceTest {

    private final SocialIdentityVerifierRegistry verifierRegistry =
            mock(SocialIdentityVerifierRegistry.class);
    private final LoginNonceService loginNonceService = mock(LoginNonceService.class);
    private final AppleAuthorizationService appleAuthorizationService =
            mock(AppleAuthorizationService.class);
    private final SocialAccountService socialAccountService = mock(SocialAccountService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final SocialLoginService service = new SocialLoginService(
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
                "login-nonce"
        );
        Member member = member(1L);
        when(verifierRegistry.verify(SocialProvider.GOOGLE, "id-token")).thenReturn(identity);
        when(socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "subject",
                "member@example.com",
                null
        )).thenReturn(new AuthenticatedMember(member, true));
        when(tokenService.issue(member)).thenReturn(tokens());

        SocialLoginResult result = service.login(command);

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

        assertThatThrownBy(() -> service.login(command))
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
                "hashed-nonce"
        );
        Member member = member(2L);
        when(verifierRegistry.verify(SocialProvider.APPLE, "id-token")).thenReturn(identity);
        when(appleAuthorizationService.exchange("authorization-code", identity))
                .thenReturn("encrypted-provider-token");
        when(socialAccountService.getOrCreate(
                SocialProvider.APPLE,
                "apple-subject",
                "member@example.com",
                "encrypted-provider-token"
        )).thenReturn(new AuthenticatedMember(member, false));
        when(tokenService.issue(member)).thenReturn(tokens());

        service.login(command);

        verify(loginNonceService).consume(
                SocialProvider.APPLE,
                "raw-nonce",
                "hashed-nonce"
        );
        verify(appleAuthorizationService).exchange("authorization-code", identity);
    }

    @Test
    void rejectsAppleLoginWithoutCodeBeforeConsumingNonce() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.APPLE,
                "id-token",
                null,
                "raw-nonce"
        );

        assertThatThrownBy(() -> service.login(command))
                .isInstanceOf(InvalidSocialLoginRequestException.class);
        verifyNoInteractions(verifierRegistry, loginNonceService);
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
