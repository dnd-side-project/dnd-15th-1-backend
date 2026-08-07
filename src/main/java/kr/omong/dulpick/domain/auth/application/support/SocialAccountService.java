package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.application.exception.ConcurrentSocialAccountCreationException;
import kr.omong.dulpick.domain.auth.application.support.model.AuthenticatedMember;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class SocialAccountService {

    private static final String PROVIDER_SUBJECT_CONSTRAINT =
            "uk_social_accounts_provider_subject";

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
        saveNewAccount(account);
        return new AuthenticatedMember(member, true);
    }

    private void saveNewAccount(SocialAccount account) {
        try {
            socialAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            if (isProviderSubjectConflict(exception)) {
                throw new ConcurrentSocialAccountCreationException(exception);
            }
            throw exception;
        }
    }

    private boolean isProviderSubjectConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return PROVIDER_SUBJECT_CONSTRAINT.equalsIgnoreCase(
                        constraintViolation.getConstraintName()
                );
            }
            cause = cause.getCause();
        }
        return false;
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
