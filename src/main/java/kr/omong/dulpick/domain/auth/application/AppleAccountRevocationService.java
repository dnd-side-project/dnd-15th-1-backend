package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import org.springframework.stereotype.Service;

@Service
public class AppleAccountRevocationService {

    private final SocialAccountRepository socialAccountRepository;
    private final AppleAuthorizationService appleAuthorizationService;

    public AppleAccountRevocationService(
            SocialAccountRepository socialAccountRepository,
            AppleAuthorizationService appleAuthorizationService
    ) {
        this.socialAccountRepository = socialAccountRepository;
        this.appleAuthorizationService = appleAuthorizationService;
    }

    public void revokeForMember(Long memberId) {
        socialAccountRepository.findAllByMemberId(memberId).stream()
                .filter(this::hasAppleRefreshToken)
                .forEach(this::revoke);
    }

    private boolean hasAppleRefreshToken(SocialAccount account) {
        return account.getProvider() == SocialProvider.APPLE
                && account.getProviderRefreshToken() != null;
    }

    private void revoke(SocialAccount account) {
        try {
            appleAuthorizationService.revoke(
                    account.getProviderRefreshToken(),
                    account.getProviderClientId()
            );
            account.clearProviderAuthorization();
        } catch (AppleAuthorizationException exception) {
            throw new AppleTokenRevocationException(exception);
        }
    }
}
