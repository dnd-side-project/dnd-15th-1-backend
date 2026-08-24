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
import java.time.Instant;
import java.util.List;

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
        Instant updatedAt = clock.instant();
        SocialAccount account = socialAccountRepository
                .findByProviderAndProviderSubject(provider, providerSubject)
                .orElse(null);
        if (account == null) {
            return create(provider, providerSubject, email, providerAuthorization, updatedAt);
        }
        if (account.getMember().isActive()) {
            return updateExisting(account, email, providerAuthorization, updatedAt);
        }
        SocialAccount lockedAccount = socialAccountRepository
                .findForUpdateByProviderAndProviderSubject(provider, providerSubject)
                .orElseThrow(() -> new IllegalStateException("Social account was not found"));
        if (lockedAccount.getMember().isActive()) {
            return updateExisting(lockedAccount, email, providerAuthorization, updatedAt);
        }
        return createReplacementMember(lockedAccount, email, providerAuthorization, updatedAt);
    }

    @Transactional
    public AuthenticatedMember getExisting(
            SocialProvider provider,
            String providerSubject
    ) {
        SocialAccount account = socialAccountRepository
                .findForUpdateByProviderAndProviderSubject(provider, providerSubject)
                .orElseThrow(() -> new IllegalStateException("Social account was not created"));
        if (account.getMember().isActive()) {
            return authenticateExisting(account);
        }
        return createReplacementMember(
                account,
                account.getEmail(),
                ProviderAuthorization.none(),
                clock.instant()
        );
    }

    private AuthenticatedMember updateExisting(
            SocialAccount account,
            String email,
            ProviderAuthorization providerAuthorization,
            Instant updatedAt
    ) {
        if (email != null) {
            account.updateEmail(email, updatedAt);
        }
        updateProviderAuthorization(account, providerAuthorization, updatedAt);
        return new AuthenticatedMember(account.getMember(), false);
    }

    private void updateProviderAuthorization(
            SocialAccount account,
            ProviderAuthorization providerAuthorization,
            Instant updatedAt
    ) {
        if (providerAuthorization.hasRefreshToken()) {
            account.updateProviderAuthorization(
                    providerAuthorization.encryptedRefreshToken(),
                    providerAuthorization.clientId(),
                    updatedAt
            );
            return;
        }
        if (providerAuthorization.hasClientId()) {
            account.updateProviderClientIdWhenTokenIsAbsent(
                    providerAuthorization.clientId(),
                    updatedAt
            );
        }
    }

    private AuthenticatedMember create(
            SocialProvider provider,
            String providerSubject,
            String email,
            ProviderAuthorization providerAuthorization,
            Instant createdAt
    ) {
        Member member = memberRepository.save(Member.create(createdAt));
        SocialAccount account = SocialAccount.create(
                member,
                provider,
                providerSubject,
                email,
                createdAt
        );
        updateProviderAuthorization(account, providerAuthorization, createdAt);
        saveNewAccount(account);
        return new AuthenticatedMember(member, true);
    }

    private AuthenticatedMember createReplacementMember(
            SocialAccount account,
            String email,
            ProviderAuthorization providerAuthorization,
            Instant createdAt
    ) {
        List<SocialAccount> accounts = socialAccountRepository
                .findAllForUpdateByMemberId(account.getMember().getId());
        Member replacement = memberRepository.save(Member.create(createdAt));
        accounts.forEach(socialAccount -> socialAccount.reassignMember(replacement, createdAt));
        SocialAccount replacementAccount = accounts.stream()
                .filter(socialAccount -> socialAccount.getProvider() == account.getProvider()
                        && socialAccount.getProviderSubject().equals(account.getProviderSubject()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Social account was not found"));
        updateExisting(replacementAccount, email, providerAuthorization, createdAt);
        return new AuthenticatedMember(replacement, true);
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
        return new AuthenticatedMember(account.getMember(), false);
    }
}
