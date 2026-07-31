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
        ProviderAuthorization providerAuthorization = exchangeAppleCode(command, identity);
        AuthenticatedMember authenticatedMember = getOrCreateMember(
                command.provider(),
                identity,
                providerAuthorization
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

    private ProviderAuthorization exchangeAppleCode(
            SocialLoginCommand command,
            SocialIdentity identity
    ) {
        if (command.provider() != SocialProvider.APPLE) {
            return ProviderAuthorization.none();
        }
        if (isBlank(command.authorizationCode())) {
            return ProviderAuthorization.clientIdOnly(identity.audience());
        }
        return appleAuthorizationService.exchange(command.authorizationCode(), identity);
    }

    private void validateRequiredFields(SocialLoginCommand command) {
        if (isBlank(command.nonce())) {
            throw new InvalidSocialLoginRequestException("Nonce is required");
        }
        if (command.provider() == null) {
            throw new InvalidSocialLoginRequestException("Provider is required");
        }
        if (isBlank(command.idToken())) {
            throw new InvalidSocialLoginRequestException("ID token is required");
        }
    }

    private AuthenticatedMember getOrCreateMember(
            SocialProvider provider,
            SocialIdentity identity,
            ProviderAuthorization providerAuthorization
    ) {
        try {
            return socialAccountService.getOrCreate(
                    provider,
                    identity.providerSubject(),
                    identity.email(),
                    providerAuthorization
            );
        } catch (DataIntegrityViolationException exception) {
            return socialAccountService.getExisting(provider, identity.providerSubject());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
