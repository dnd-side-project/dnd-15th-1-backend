package kr.omong.dulpick.domain.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderSubject(
            SocialProvider provider,
            String providerSubject
    );
}
