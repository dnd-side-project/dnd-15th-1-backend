package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.application.command.SocialLoginCommand;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;
import kr.omong.dulpick.domain.auth.application.exception.InvalidSocialLoginRequestException;
import kr.omong.dulpick.domain.auth.application.exception.ConcurrentSocialAccountCreationException;
import kr.omong.dulpick.domain.auth.application.support.AppleAuthorizationService;
import kr.omong.dulpick.domain.auth.application.support.LoginNonceService;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.SocialIdentityVerifierRegistry;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.AuthenticatedMember;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class SocialLoginHandler {

    private final SocialIdentityVerifierRegistry verifierRegistry;
    private final LoginNonceService loginNonceService;
    private final AppleAuthorizationService appleAuthorizationService;
    private final SocialAccountService socialAccountService;
    private final MemberProfileRepository memberProfileRepository;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public SocialLoginHandler(
            SocialIdentityVerifierRegistry verifierRegistry,
            LoginNonceService loginNonceService,
            AppleAuthorizationService appleAuthorizationService,
            SocialAccountService socialAccountService,
            MemberProfileRepository memberProfileRepository,
            TokenService tokenService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.verifierRegistry = verifierRegistry;
        this.loginNonceService = loginNonceService;
        this.appleAuthorizationService = appleAuthorizationService;
        this.socialAccountService = socialAccountService;
        this.memberProfileRepository = memberProfileRepository;
        this.tokenService = tokenService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public SocialLoginHandler(
            SocialIdentityVerifierRegistry verifierRegistry,
            LoginNonceService loginNonceService,
            AppleAuthorizationService appleAuthorizationService,
            SocialAccountService socialAccountService,
            MemberProfileRepository memberProfileRepository,
            TokenService tokenService
    ) {
        this(
                verifierRegistry,
                loginNonceService,
                appleAuthorizationService,
                socialAccountService,
                memberProfileRepository,
                tokenService,
                null,
                Clock.systemUTC()
        );
    }

    public SocialLoginResult handle(SocialLoginCommand command) {
        validateRequiredFields(command);
        SocialIdentity identity = verifierRegistry.verify(command.provider(), command.idToken());
        validateNonce(command, identity);
        ProviderAuthorization providerAuthorization = exchangeAppleCode(command, identity);
        AuthenticatedMember authenticatedMember = getOrCreateMember(
                command.provider(),
                identity,
                providerAuthorization
        );
        publishSignupEvent(authenticatedMember);
        IssuedTokens tokens = tokenService.issue(authenticatedMember.member());
        return new SocialLoginResult(
                authenticatedMember.member().getId(),
                authenticatedMember.newMember(),
                isOnboardingCompleted(authenticatedMember),
                tokens
        );
    }

    private void publishSignupEvent(AuthenticatedMember authenticatedMember) {
        if (!authenticatedMember.newMember() || eventPublisher == null) {
            return;
        }
        Long memberId = authenticatedMember.member().getId();
        if (memberId == null) {
            return;
        }
        eventPublisher.publishEvent(new AnalyticsActionEvent(
                "MEMBER_SIGNED_UP:%d".formatted(memberId),
                AnalyticsEventType.MEMBER_SIGNED_UP,
                memberId,
                null,
                "MEMBER",
                memberId,
                clock.instant()
        ));
    }

    private boolean isOnboardingCompleted(AuthenticatedMember authenticatedMember) {
        return !authenticatedMember.newMember()
                && memberProfileRepository.existsById(authenticatedMember.member().getId());
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
        } catch (ConcurrentSocialAccountCreationException exception) {
            return socialAccountService.getExisting(provider, identity.providerSubject());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
