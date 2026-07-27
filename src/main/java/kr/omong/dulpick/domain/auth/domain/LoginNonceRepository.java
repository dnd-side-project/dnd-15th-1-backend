package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LoginNonceRepository extends JpaRepository<LoginNonce, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginNonce> findByProviderAndNonceHash(SocialProvider provider, String nonceHash);
}
