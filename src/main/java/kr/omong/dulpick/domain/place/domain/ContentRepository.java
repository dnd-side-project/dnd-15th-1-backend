package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findByCanonicalUrlHash(String canonicalUrlHash);

    List<Content> findAllByPublicationStatusOrderByCreatedAtDesc(ContentPublicationStatus status);

    Page<Content> findAllByPublicationStatus(ContentPublicationStatus status, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO contents
                (canonical_url, canonical_url_hash, source_type, title, content,
                 thumbnail_url, content_hash, publication_status, created_at, updated_at)
            VALUES
                (:url, :urlHash, :sourceType, :title, :body,
                 :thumbnail, :contentHash, 'PENDING', :now, :now)
            ON DUPLICATE KEY UPDATE canonical_url_hash = canonical_url_hash
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("url") String url,
            @Param("urlHash") String urlHash,
            @Param("sourceType") String sourceType,
            @Param("title") String title,
            @Param("body") String body,
            @Param("thumbnail") String thumbnail,
            @Param("contentHash") String contentHash,
            @Param("now") java.time.Instant now
    );

    @Modifying
    @Query("""
            UPDATE Content content
               SET content.analysisStatus = 'PROCESSING',
                   content.analysisStartedAt = :now,
                   content.analysisClaimToken = :claimToken
             WHERE content.id = :contentId
               AND (content.analysisStatus IS NULL
                    OR content.analysisStatus = 'FAILED'
                    OR (content.analysisStatus = 'READY'
                        AND (content.analysisContentHash <> :contentHash
                             OR content.analyzerModel <> :analyzerModel
                             OR content.promptVersion <> :promptVersion))
                    OR (content.analysisStatus = 'PROCESSING'
                        AND content.analysisStartedAt < :staleBefore))
            """)
    int claimAnalysis(
            @Param("contentId") Long contentId,
            @Param("contentHash") String contentHash,
            @Param("analyzerModel") String analyzerModel,
            @Param("promptVersion") String promptVersion,
            @Param("claimToken") String claimToken,
            @Param("now") java.time.Instant now,
            @Param("staleBefore") java.time.Instant staleBefore
    );
}
