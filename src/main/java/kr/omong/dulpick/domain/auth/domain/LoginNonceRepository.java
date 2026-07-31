package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface LoginNonceRepository extends JpaRepository<LoginNonce, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginNonce> findByProviderAndNonceHash(SocialProvider provider, String nonceHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    DELETE FROM login_nonces
                    WHERE used_at IS NOT NULL
                      AND used_at <= :usedBefore
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    int deleteUsedBefore(
            @Param("usedBefore") Instant usedBefore,
            @Param("batchSize") int batchSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    DELETE FROM login_nonces
                    WHERE expires_at <= :expiredBefore
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    int deleteExpiredBefore(
            @Param("expiredBefore") Instant expiredBefore,
            @Param("batchSize") int batchSize
    );
}
