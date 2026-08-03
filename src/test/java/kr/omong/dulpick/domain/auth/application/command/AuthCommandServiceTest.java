package kr.omong.dulpick.domain.auth.application.command;

import kr.omong.dulpick.domain.auth.application.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.application.SocialLoginResult;
import kr.omong.dulpick.domain.auth.application.command.handler.IssueNonceHandler;
import kr.omong.dulpick.domain.auth.application.command.handler.LogoutHandler;
import kr.omong.dulpick.domain.auth.application.command.handler.ReissueTokenHandler;
import kr.omong.dulpick.domain.auth.application.command.handler.SocialLoginHandler;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthCommandServiceTest {

    private final IssueNonceHandler issueNonceHandler = mock(IssueNonceHandler.class);
    private final SocialLoginHandler socialLoginHandler = mock(SocialLoginHandler.class);
    private final ReissueTokenHandler reissueTokenHandler = mock(ReissueTokenHandler.class);
    private final LogoutHandler logoutHandler = mock(LogoutHandler.class);
    private final AuthCommandService service = new AuthCommandService(
            issueNonceHandler,
            socialLoginHandler,
            reissueTokenHandler,
            logoutHandler
    );

    @Test
    void delegatesAuthenticationCommandsToHandlers() {
        SocialLoginCommand command = new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "id-token",
                null,
                "nonce"
        );
        IssuedNonce nonce = new IssuedNonce("nonce", Instant.EPOCH);
        IssuedTokens tokens = new IssuedTokens("access", "refresh", 900);
        SocialLoginResult loginResult = new SocialLoginResult(1L, true, tokens);
        when(issueNonceHandler.handle(SocialProvider.GOOGLE)).thenReturn(nonce);
        when(socialLoginHandler.handle(command)).thenReturn(loginResult);
        when(reissueTokenHandler.handle("refresh")).thenReturn(tokens);

        assertThat(service.issueNonce(SocialProvider.GOOGLE)).isEqualTo(nonce);
        assertThat(service.socialLogin(command)).isEqualTo(loginResult);
        assertThat(service.reissue("refresh")).isEqualTo(tokens);
        service.logout("refresh", 1L);

        verify(logoutHandler).handle("refresh", 1L);
    }
}
