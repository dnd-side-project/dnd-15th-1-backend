package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT content FROM Content content WHERE content.id = :contentId")
    java.util.Optional<Content> findByIdForUpdate(@Param("contentId") Long contentId);

    Optional<Content> findByCanonicalUrlHash(String canonicalUrlHash);

    Optional<Content> findByIdAndPublicationStatus(
            Long id,
            ContentPublicationStatus publicationStatus
    );

    List<Content> findAllByPublicationStatusOrderByCreatedAtDesc(ContentPublicationStatus status);

    List<Content> findAllBySourceTypeInOrderByIdAsc(List<ContentSourceType> sourceTypes);

    @Query(
            value = """
                    SELECT content
                    FROM Content content
                    WHERE content.publicationStatus = :status
                      AND EXISTS (
                            SELECT 1
                            FROM ContentPlace contentPlace
                            WHERE contentPlace.contentId = content.id
                              AND contentPlace.placeId = :placeId
                      )
                    ORDER BY content.createdAt DESC, content.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(content)
                    FROM Content content
                    WHERE content.publicationStatus = :status
                      AND EXISTS (
                            SELECT 1
                            FROM ContentPlace contentPlace
                            WHERE contentPlace.contentId = content.id
                              AND contentPlace.placeId = :placeId
                      )
                    """
    )
    Page<Content> findAllByPlaceIdAndPublicationStatus(
            @Param("placeId") Long placeId,
            @Param("status") ContentPublicationStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT content
            FROM Content content
            WHERE content.publicationStatus = :status
              AND EXISTS (
                    SELECT 1
                    FROM ContentPlace contentPlace
                    WHERE contentPlace.contentId = content.id
                      AND contentPlace.placeId = :placeId
              )
            ORDER BY content.createdAt DESC, content.id DESC
            """)
    List<Content> findAllByPlaceIdAndPublicationStatus(
            @Param("placeId") Long placeId,
            @Param("status") ContentPublicationStatus status
    );

    Page<Content> findAllByPublicationStatus(ContentPublicationStatus status, Pageable pageable);

    @Query(
            value = """
                    SELECT *
                    FROM contents
                    WHERE publication_status = :status
                      AND MATCH(title, content) AGAINST(:query IN BOOLEAN MODE)
                    ORDER BY created_at DESC, id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM contents
                    WHERE publication_status = :status
                      AND MATCH(title, content) AGAINST(:query IN BOOLEAN MODE)
                    """,
            nativeQuery = true
    )
    Page<Content> searchByPublicationStatusAndKeyword(
            @Param("status") String status,
            @Param("query") String query,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT *
                    FROM contents
                    WHERE publication_status = :status
                      AND MATCH(title, content) AGAINST(:query IN BOOLEAN MODE)
                    ORDER BY created_at DESC, id DESC
                    """,
            nativeQuery = true
    )
    List<Content> searchAllByPublicationStatusAndKeyword(
            @Param("status") String status,
            @Param("query") String query
    );

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
                        AND (content.analysisContentHash IS NULL
                             OR content.analysisContentHash <> :contentHash
                             OR content.analyzerModel IS NULL
                             OR content.analyzerModel <> :analyzerModel
                             OR content.promptVersion IS NULL
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

    @Modifying
    @Query("""
            UPDATE Content content
               SET content.analysisContentHash = :contentHash,
                   content.analyzerModel = :analyzerModel,
                   content.promptVersion = :promptVersion,
                   content.extractedCandidatesJson = :candidatesJson,
                   content.analyzedAt = :analyzedAt,
                   content.analysisStatus = 'READY',
                   content.analysisStartedAt = null,
                   content.analysisClaimToken = null
             WHERE content.id = :contentId
               AND content.analysisStatus = 'PROCESSING'
               AND content.analysisClaimToken = :claimToken
            """)
    int completeAnalysis(
            @Param("contentId") Long contentId,
            @Param("claimToken") String claimToken,
            @Param("contentHash") String contentHash,
            @Param("analyzerModel") String analyzerModel,
            @Param("promptVersion") String promptVersion,
            @Param("candidatesJson") String candidatesJson,
            @Param("analyzedAt") java.time.Instant analyzedAt
    );

    @Modifying
    @Query("""
            UPDATE Content content
               SET content.analysisStatus = 'FAILED',
                   content.analysisStartedAt = null,
                   content.analysisClaimToken = null
             WHERE content.id = :contentId
               AND content.analysisStatus = 'PROCESSING'
               AND content.analysisClaimToken = :claimToken
            """)
    int failAnalysis(
            @Param("contentId") Long contentId,
            @Param("claimToken") String claimToken
    );

    @Modifying
    @Query("""
            UPDATE Content content
               SET content.analysisContentHash = null,
                   content.analyzerModel = null,
                   content.promptVersion = null,
                   content.analysisStatus = 'FAILED',
                   content.analysisStartedAt = null,
                   content.analysisClaimToken = null,
                   content.analyzedAt = null,
                   content.extractedCandidatesJson = null
             WHERE content.id = :contentId
               AND content.analysisStatus = 'READY'
            """)
    int invalidateCachedAnalysis(@Param("contentId") Long contentId);
}
