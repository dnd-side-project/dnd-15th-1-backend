package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlaceImportRepository extends JpaRepository<PlaceImport, Long> {

    Optional<PlaceImport> findByMemberIdAndCanonicalUrlHash(
            Long memberId,
            String canonicalUrlHash
    );

    long countByMemberIdAndCreatedAtGreaterThanEqual(
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
                placeImport.failureCode = null,
                placeImport.updatedAt = :now
            WHERE placeImport.id = :importId
              AND (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED
                   OR placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.FAILED
                   OR (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
                       AND placeImport.updatedAt < :staleBefore))
            """)
    int claimRetryable(
            @Param("importId") Long importId,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore
    );

    @Modifying
    @Query("""
            UPDATE PlaceImport placeImport
            SET placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.RECEIVED,
                placeImport.failureCode = null,
                placeImport.updatedAt = :now
            WHERE placeImport.id = :importId
              AND (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.FAILED
                   OR (placeImport.status = kr.omong.dulpick.domain.place.domain.PlaceImportStatus.PROCESSING
                       AND placeImport.updatedAt < :staleBefore))
            """)
    int requeueRetryable(
            @Param("importId") Long importId,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore
    );

}
