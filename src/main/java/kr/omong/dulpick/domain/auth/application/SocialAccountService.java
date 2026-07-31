package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public SocialAccountService(
            SocialAccountRepository socialAccountRepository,
            MemberRepository memberRepository,
            Clock clock
    ) {
        this.socialAccountRepository = socialAccountRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Transactional
    public AuthenticatedMember getOrCreate(
            SocialProvider provider,
            String providerSubject,
            String email,
            ProviderAuthorization providerAuthorization
    ) {
        return socialAccountRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(account -> updateExisting(account, email, providerAuthorization))
                .orElseGet(() -> create(
                        provider,
                        providerSubject,
                        email,
                        providerAuthorization
                ));
    }

    @Transactional
    public AuthenticatedMember getExisting(
            SocialProvider provider,
            String providerSubject
    ) {
        return socialAccountRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(this::authenticateExisting)
                .orElseThrow(() -> new IllegalStateException("Social account was not created"));
    }

    private AuthenticatedMember updateExisting(
            SocialAccount account,
            String email,
            ProviderAuthorization providerAuthorization
    ) {
        rejoinIfWithdrawn(account.getMember());
        if (email != null) {
            account.updateEmail(email);
        }
        updateProviderAuthorization(account, providerAuthorization);
        return new AuthenticatedMember(account.getMember(), false);
    }

    private void updateProviderAuthorization(
            SocialAccount account,
            ProviderAuthorization providerAuthorization
    ) {
        if (providerAuthorization.hasRefreshToken()) {
            account.updateProviderAuthorization(
                    providerAuthorization.encryptedRefreshToken(),
                    providerAuthorization.clientId()
            );
            return;
        }
        if (providerAuthorization.hasClientId()) {
            account.updateProviderClientIdWhenTokenIsAbsent(
                    providerAuthorization.clientId()
            );
        }
    }

    private AuthenticatedMember create(
            SocialProvider provider,
            String providerSubject,
            String email,
            ProviderAuthorization providerAuthorization
    ) {
        Member member = memberRepository.save(Member.create());
        SocialAccount account = SocialAccount.create(member, provider, providerSubject, email);
        updateProviderAuthorization(account, providerAuthorization);
        socialAccountRepository.saveAndFlush(account);
        return new AuthenticatedMember(member, true);
    }

    private AuthenticatedMember authenticateExisting(SocialAccount account) {
        rejoinIfWithdrawn(account.getMember());
        return new AuthenticatedMember(account.getMember(), false);
    }

    private void rejoinIfWithdrawn(Member member) {
        if (member.isActive()) {
            return;
        }
        member.rejoin(clock.instant());
    }
}
