package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderSubject(
            SocialProvider provider,
            String providerSubject
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account
            from SocialAccount account
            where account.provider = :provider
              and account.providerSubject = :providerSubject
            """)
    Optional<SocialAccount> findForUpdateByProviderAndProviderSubject(
            @Param("provider") SocialProvider provider,
            @Param("providerSubject") String providerSubject
    );

    List<SocialAccount> findAllByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account
            from SocialAccount account
            where account.member.id = :memberId
            """)
    List<SocialAccount> findAllForUpdateByMemberId(@Param("memberId") Long memberId);
}
