package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialAccountService {

    private final SocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;

    public SocialAccountService(
            SocialAccountRepository socialAccountRepository,
            MemberRepository memberRepository
    ) {
        this.socialAccountRepository = socialAccountRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public AuthenticatedMember getOrCreate(
            SocialProvider provider,
            String providerSubject,
            String email,
            String encryptedProviderRefreshToken
    ) {
        return socialAccountRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(account -> updateExisting(account, email, encryptedProviderRefreshToken))
                .orElseGet(() -> create(
                        provider,
                        providerSubject,
                        email,
                        encryptedProviderRefreshToken
                ));
    }

    @Transactional(readOnly = true)
    public AuthenticatedMember getExisting(
            SocialProvider provider,
            String providerSubject
    ) {
        return socialAccountRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(account -> new AuthenticatedMember(account.getMember(), false))
                .orElseThrow(() -> new IllegalStateException("Social account was not created"));
    }

    private AuthenticatedMember updateExisting(
            SocialAccount account,
            String email,
            String encryptedProviderRefreshToken
    ) {
        if (email != null) {
            account.updateEmail(email);
        }
        if (encryptedProviderRefreshToken != null) {
            account.updateProviderRefreshToken(encryptedProviderRefreshToken);
        }
        return new AuthenticatedMember(account.getMember(), false);
    }

    private AuthenticatedMember create(
            SocialProvider provider,
            String providerSubject,
            String email,
            String encryptedProviderRefreshToken
    ) {
        Member member = memberRepository.save(Member.create());
        SocialAccount account = SocialAccount.create(member, provider, providerSubject, email);
        if (encryptedProviderRefreshToken != null) {
            account.updateProviderRefreshToken(encryptedProviderRefreshToken);
        }
        socialAccountRepository.saveAndFlush(account);
        return new AuthenticatedMember(member, true);
    }
}
