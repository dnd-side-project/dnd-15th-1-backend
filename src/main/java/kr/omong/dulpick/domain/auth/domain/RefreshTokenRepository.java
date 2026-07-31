package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshToken token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.member.id = :memberId
              AND token.revokedAt IS NULL
            """)
    int revokeAllByMemberId(
            @Param("memberId") Long memberId,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    DELETE FROM refresh_tokens
                    WHERE expires_at <= :expiredBefore
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    int deleteExpiredBefore(
            @Param("expiredBefore") Instant expiredBefore,
            @Param("batchSize") int batchSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    DELETE FROM refresh_tokens
                    WHERE revoked_at IS NOT NULL
                      AND revoked_at <= :revokedBefore
                      AND replaced_by_token_hash IS NULL
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    int deleteRevokedBeforeWithoutRotation(
            @Param("revokedBefore") Instant revokedBefore,
            @Param("batchSize") int batchSize
    );
}
