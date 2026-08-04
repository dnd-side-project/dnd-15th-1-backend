package kr.omong.dulpick.domain.auth.application.command;

import kr.omong.dulpick.domain.auth.application.command.handler.IssueNonceHandler;
import kr.omong.dulpick.domain.auth.application.command.handler.LogoutHandler;
import kr.omong.dulpick.domain.auth.application.command.handler.ReissueTokenHandler;
import kr.omong.dulpick.domain.auth.application.command.handler.SocialLoginHandler;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthCommandService {

    private final IssueNonceHandler issueNonceHandler;
    private final SocialLoginHandler socialLoginHandler;
    private final ReissueTokenHandler reissueTokenHandler;
    private final LogoutHandler logoutHandler;

    public AuthCommandService(
            IssueNonceHandler issueNonceHandler,
            SocialLoginHandler socialLoginHandler,
            ReissueTokenHandler reissueTokenHandler,
            LogoutHandler logoutHandler
    ) {
        this.issueNonceHandler = issueNonceHandler;
        this.socialLoginHandler = socialLoginHandler;
        this.reissueTokenHandler = reissueTokenHandler;
        this.logoutHandler = logoutHandler;
    }

    public IssuedNonce issueNonce(SocialProvider provider) {
        return issueNonceHandler.handle(provider);
    }

    public SocialLoginResult socialLogin(SocialLoginCommand command) {
        return socialLoginHandler.handle(command);
    }

    public IssuedTokens reissue(String refreshToken) {
        return reissueTokenHandler.handle(refreshToken);
    }

    public void logout(String refreshToken, Long memberId) {
        logoutHandler.handle(refreshToken, memberId);
    }
}
