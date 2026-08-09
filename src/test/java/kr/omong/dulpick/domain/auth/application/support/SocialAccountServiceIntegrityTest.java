package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.application.exception.ConcurrentSocialAccountCreationException;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialAccountServiceIntegrityTest {

    private final SocialAccountRepository accountRepository =
            mock(SocialAccountRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountService service = new SocialAccountService(
            accountRepository,
            memberRepository,
            Clock.systemUTC()
    );

    @Test
    void translatesOnlyProviderSubjectUniqueConflict() {
        prepareNewMember();
        DataIntegrityViolationException conflict = violation(
                "uk_social_accounts_provider_subject"
        );
        when(accountRepository.saveAndFlush(any(SocialAccount.class))).thenThrow(conflict);

        assertThatThrownBy(this::createAccount)
                .isInstanceOf(ConcurrentSocialAccountCreationException.class)
                .hasCause(conflict);
    }

    @Test
    void propagatesUnexpectedDatabaseConstraint() {
        prepareNewMember();
        DataIntegrityViolationException violation = violation("fk_social_accounts_member");
        when(accountRepository.saveAndFlush(any(SocialAccount.class))).thenThrow(violation);

        assertThatThrownBy(this::createAccount).isSameAs(violation);
    }

    private void prepareNewMember() {
        when(accountRepository.findByProviderAndProviderSubject(
                SocialProvider.GOOGLE,
                "subject"
        )).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void createAccount() {
        service.getOrCreate(
                SocialProvider.GOOGLE,
                "subject",
                "member@example.com",
                ProviderAuthorization.none()
        );
    }

    private DataIntegrityViolationException violation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "constraint violation",
                new SQLException("constraint violation"),
                constraintName
        );
        return new DataIntegrityViolationException("constraint violation", cause);
    }
}
