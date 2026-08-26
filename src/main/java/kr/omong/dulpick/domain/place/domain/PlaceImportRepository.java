package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlaceImportRepository extends JpaRepository<PlaceImport, Long> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT placeImport FROM PlaceImport placeImport WHERE placeImport.id = :importId")
    java.util.Optional<PlaceImport> findByIdForUpdate(@Param("importId") Long importId);

    Optional<PlaceImport> findByMemberIdAndCanonicalUrlHash(
            Long memberId,
            String canonicalUrlHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT placeImport
              FROM PlaceImport placeImport
             WHERE placeImport.id = :importId
               AND placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
               AND placeImport.processingClaimToken = :claimToken
            """)
    Optional<PlaceImport> findClaimedForUpdate(
            @Param("importId") Long importId,
            @Param("claimToken") String claimToken
    );

    @Query(value = """
            SELECT COUNT(*)
              FROM place_imports place_import
             WHERE place_import.member_id = :memberId
               AND place_import.created_at >= :createdAt
               AND NOT EXISTS (
                   SELECT 1
                     FROM contents content
                    WHERE content.id = place_import.content_id
                      AND content.content_hash IS NOT NULL
                      AND content.content_hash = place_import.content_hash
               )
            """, nativeQuery = true)
    long countAnalysisRequiredByMemberIdAndCreatedAtGreaterThanEqual(
            Long memberId,
            Instant createdAt
    );

    @Query("""
            SELECT placeImport.id
              FROM PlaceImport placeImport
             WHERE (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED
                    AND placeImport.updatedAt <= :receivedBefore)
                OR (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
                    AND placeImport.updatedAt < :processingBefore)
             ORDER BY placeImport.updatedAt ASC, placeImport.id ASC
            """)
    List<Long> findRecoverableIds(
            @Param("receivedBefore") Instant receivedBefore,
            @Param("processingBefore") Instant processingBefore,
            Pageable pageable
    );

    @Modifying
    @Query(value = """
            INSERT INTO place_imports
                (member_id, canonical_url, canonical_url_hash, source_type, status,
                 retry_count, created_at, updated_at)
            VALUES (:memberId, :url, :urlHash, :sourceType, 'RECEIVED', 0, :now, :now)
            ON DUPLICATE KEY UPDATE canonical_url_hash = canonical_url_hash
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("memberId") Long memberId,
            @Param("url") String url,
            @Param("urlHash") String urlHash,
            @Param("sourceType") String sourceType,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
            SET placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING,
                placeImport.processingClaimToken = :claimToken,
                placeImport.failureCode = null,
                placeImport.updatedAt = :now
            WHERE placeImport.id = :importId
              AND (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED
                   OR (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
                       AND placeImport.updatedAt < :staleBefore))
            """)
    int claimPending(
            @Param("importId") Long importId,
            @Param("claimToken") String claimToken,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
            SET placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED,
                placeImport.processingClaimToken = null,
                placeImport.failureCode = null,
                placeImport.updatedAt = :now
            WHERE placeImport.id = :importId
              AND ((placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.FAILED
                    AND placeImport.retryCount < :maxRetryCount
                    AND placeImport.updatedAt <= :retryBefore)
                   OR (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
                       AND placeImport.updatedAt < :staleBefore))
            """)
    int requeueRetryable(
            @Param("importId") Long importId,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("retryBefore") Instant retryBefore,
            @Param("maxRetryCount") int maxRetryCount
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
               SET placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED,
                   placeImport.processingClaimToken = null,
                   placeImport.failureCode = null,
                   placeImport.updatedAt = :now
             WHERE placeImport.id = :importId
               AND (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.FAILED
                    OR (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
                        AND placeImport.updatedAt < :staleBefore))
            """)
    int adminRequeue(
            @Param("importId") Long importId,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
               SET placeImport.updatedAt = :now
             WHERE placeImport.id = :importId
               AND placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
               AND placeImport.processingClaimToken = :claimToken
            """)
    int heartbeatClaim(
            @Param("importId") Long importId,
            @Param("claimToken") String claimToken,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
               SET placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED,
                   placeImport.processingClaimToken = null,
                   placeImport.failureCode = null,
                   placeImport.updatedAt = :now
             WHERE placeImport.id = :importId
               AND placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
               AND placeImport.processingClaimToken = :claimToken
            """)
    int requeueClaimed(
            @Param("importId") Long importId,
            @Param("claimToken") String claimToken,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
               SET placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.FAILED,
                   placeImport.processingClaimToken = null,
                   placeImport.failureCode = :failureCode,
                   placeImport.retryCount = placeImport.retryCount + 1,
                   placeImport.updatedAt = :now
             WHERE placeImport.id = :importId
               AND placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
               AND placeImport.processingClaimToken = :claimToken
            """)
    int failClaimed(
            @Param("importId") Long importId,
            @Param("claimToken") String claimToken,
            @Param("failureCode") String failureCode,
            @Param("now") Instant now
    );

}
