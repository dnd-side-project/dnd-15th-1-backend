package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceImportRepository extends JpaRepository<PlaceImport, Long> {

    Optional<PlaceImport> findByMemberIdAndCanonicalUrlHash(
            Long memberId,
            String canonicalUrlHash
    );

    long countByMemberIdAndCreatedAtGreaterThanEqual(
            Long memberId,
            Instant createdAt
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
}
