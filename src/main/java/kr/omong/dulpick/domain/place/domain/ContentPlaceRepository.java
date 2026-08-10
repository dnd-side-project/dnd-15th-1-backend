package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentPlaceRepository extends JpaRepository<ContentPlace, Long> {

    boolean existsByContentIdAndPlaceId(Long contentId, Long placeId);

    List<ContentPlace> findAllByContentId(Long contentId);

    List<ContentPlace> findAllByContentIdIn(List<Long> contentIds);

    void deleteAllByContentId(Long contentId);

    @Modifying
    @Query(value = """
            INSERT INTO content_places (content_id, place_id, created_at)
            VALUES (:contentId, :placeId, :createdAt)
            ON DUPLICATE KEY UPDATE content_id = content_id
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("contentId") Long contentId,
            @Param("placeId") Long placeId,
            @Param("createdAt") java.time.Instant createdAt
    );
}
