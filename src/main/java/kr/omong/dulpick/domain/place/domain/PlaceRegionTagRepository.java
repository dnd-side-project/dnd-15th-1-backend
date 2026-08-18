package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PlaceRegionTagRepository extends JpaRepository<PlaceRegionTag, Long> {

    List<PlaceRegionTag> findAllByPlaceId(Long placeId);

    List<PlaceRegionTag> findAllByPlaceIdIn(List<Long> placeIds);

    List<PlaceRegionTag> findAllByRegionTagId(Long regionTagId);

    long countByRegionTagId(Long regionTagId);

    @Modifying
    @Query(value = """
            INSERT INTO place_region_tags (place_id, region_tag_id, created_at)
            VALUES (:placeId, :regionTagId, :createdAt)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("placeId") Long placeId,
            @Param("regionTagId") Long regionTagId,
            @Param("createdAt") Instant createdAt
    );
}
