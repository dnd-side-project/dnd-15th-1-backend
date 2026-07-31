package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class SocialLoginService {

    private final SocialIdentityVerifierRegistry verifierRegistry;
    private final LoginNonceService loginNonceService;
    private final AppleAuthorizationService appleAuthorizationService;
    private final SocialAccountService socialAccountService;
    private final TokenService tokenService;

    public SocialLoginService(
            SocialIdentityVerifierRegistry verifierRegistry,
            LoginNonceService loginNonceService,
            AppleAuthorizationService appleAuthorizationService,
            SocialAccountService socialAccountService,
            TokenService tokenService
    ) {
        this.verifierRegistry = verifierRegistry;
        this.loginNonceService = loginNonceService;
        this.appleAuthorizationService = appleAuthorizationService;
        this.socialAccountService = socialAccountService;
        this.tokenService = tokenService;
    }

    public SocialLoginResult login(SocialLoginCommand command) {
        validateRequiredFields(command);
        SocialIdentity identity = verifierRegistry.verify(command.provider(), command.idToken());
        validateNonce(command, identity);
        String providerRefreshToken = exchangeAppleCode(command, identity);
        AuthenticatedMember authenticatedMember = getOrCreateMember(
                command.provider(),
                identity,
                providerRefreshToken
        );
        IssuedTokens tokens = tokenService.issue(authenticatedMember.member());
        return new SocialLoginResult(
                authenticatedMember.member().getId(),
                authenticatedMember.newMember(),
                tokens
        );
    }

    private void validateNonce(SocialLoginCommand command, SocialIdentity identity) {
        loginNonceService.consume(command.provider(), command.nonce(), identity.tokenNonce());
    }

    private String exchangeAppleCode(
            SocialLoginCommand command,
            SocialIdentity identity
    ) {
        if (command.provider() != SocialProvider.APPLE) {
            return null;
        }
        return appleAuthorizationService.exchange(command.authorizationCode(), identity);
    }

    private void validateRequiredFields(SocialLoginCommand command) {
        if (isBlank(command.nonce())) {
            throw new InvalidSocialLoginRequestException("Nonce is required");
        }
        if (command.provider() == SocialProvider.APPLE
                && isBlank(command.authorizationCode())) {
            throw new InvalidSocialLoginRequestException("Apple authorization code is required");
        }
    }

    private AuthenticatedMember getOrCreateMember(
            SocialProvider provider,
            SocialIdentity identity,
            String providerRefreshToken
    ) {
        try {
            return socialAccountService.getOrCreate(
                    provider,
                    identity.providerSubject(),
                    identity.email(),
                    providerRefreshToken
            );
        } catch (DataIntegrityViolationException exception) {
            return socialAccountService.getExisting(provider, identity.providerSubject());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
