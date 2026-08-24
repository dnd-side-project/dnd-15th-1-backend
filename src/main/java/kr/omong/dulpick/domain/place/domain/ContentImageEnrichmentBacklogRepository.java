package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface ContentImageEnrichmentBacklogRepository
        extends JpaRepository<ContentImageEnrichmentBacklog, Long> {

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO content_image_enrichment_backlogs
                (content_id, source_urls, attempt_count, status,
                 next_attempt_at, created_at, updated_at)
            VALUES (:contentId, :sourceUrls, 1, 'PENDING',
                    :nextAttemptAt, :now, :now)
            ON DUPLICATE KEY UPDATE
                source_urls = :sourceUrls,
                status = 'PENDING',
                next_attempt_at = :nextAttemptAt,
                updated_at = :now
            """, nativeQuery = true)
    void enqueue(
            @Param("contentId") Long contentId,
            @Param("sourceUrls") String sourceUrls,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("now") Instant now
    );

    @Query("""
            SELECT backlog
              FROM ContentImageEnrichmentBacklog backlog
             WHERE (backlog.status = 'PENDING'
                    AND backlog.nextAttemptAt <= :now)
                OR (backlog.status = 'PROCESSING'
                    AND backlog.updatedAt <= :staleBefore)
             ORDER BY backlog.nextAttemptAt ASC, backlog.id ASC
            """)
    List<ContentImageEnrichmentBacklog> findRecoverable(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable
    );

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE content_image_enrichment_backlogs
               SET status = 'PROCESSING',
                   updated_at = CURRENT_TIMESTAMP(6)
             WHERE content_id = :contentId
               AND (status = 'PENDING'
                    OR (status = 'PROCESSING' AND updated_at <= :staleBefore))
            """, nativeQuery = true)
    int claim(
            @Param("contentId") Long contentId,
            @Param("staleBefore") Instant staleBefore
    );

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE content_image_enrichment_backlogs
               SET attempt_count = attempt_count + 1,
                   status = 'PENDING',
                   next_attempt_at = :nextAttemptAt,
                   updated_at = :now
             WHERE content_id = :contentId
               AND status IN ('PENDING', 'PROCESSING')
            """, nativeQuery = true)
    void scheduleRetry(
            @Param("contentId") Long contentId,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("now") Instant now
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM ContentImageEnrichmentBacklog backlog WHERE backlog.contentId = :contentId")
    void deleteByContentId(@Param("contentId") Long contentId);
}
