package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface PlaceImageEnrichmentBacklogRepository
        extends JpaRepository<PlaceImageEnrichmentBacklog, Long> {

    Optional<PlaceImageEnrichmentBacklog> findByPlaceId(Long placeId);

    List<PlaceImageEnrichmentBacklog> findByStatusAndLastFailedAtBeforeOrderByLastFailedAtAsc(
            String status,
            Instant before,
            Pageable pageable
    );

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO place_image_enrichment_backlogs
                (place_id, kakao_place_id, reason, attempt_count, status,
                 first_failed_at, last_failed_at, created_at, updated_at)
            VALUES
                (:placeId, :kakaoPlaceId, :reason, 1, 'PENDING',
                 :failedAt, :failedAt, :failedAt, :failedAt)
            ON DUPLICATE KEY UPDATE
                kakao_place_id = :kakaoPlaceId,
                reason = :reason,
                attempt_count = attempt_count + 1,
                status = 'PENDING',
                last_failed_at = :failedAt,
                updated_at = :failedAt
            """, nativeQuery = true)
    void recordFailure(
            @Param("placeId") Long placeId,
            @Param("kakaoPlaceId") String kakaoPlaceId,
            @Param("reason") String reason,
            @Param("failedAt") Instant failedAt
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM PlaceImageEnrichmentBacklog backlog WHERE backlog.placeId = :placeId")
    void deleteByPlaceId(@Param("placeId") Long placeId);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE place_image_enrichment_backlogs
               SET status = 'FAILED', updated_at = :now
             WHERE place_id = :placeId AND status = 'PENDING'
            """, nativeQuery = true)
    void markFailed(
            @Param("placeId") Long placeId,
            @Param("now") Instant now
    );
}
