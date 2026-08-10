package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

    @Query("""
            SELECT place
            FROM Place place
            WHERE place.name = :name
              AND (place.address = :addressHint OR place.roadAddress = :addressHint)
            ORDER BY place.id
            """)
    Optional<Place> findFirstByNameAndAddressHint(
            @Param("name") String name,
            @Param("addressHint") String addressHint
    );

    @Modifying
    @Query(value = """
            INSERT INTO places
                (kakao_place_id, name, address, road_address, latitude, longitude,
                 category, category_group_code, thumbnail_url, created_at, updated_at)
            VALUES
                (:kakaoId, :name, :address, :roadAddress, :latitude, :longitude,
                 :category, :categoryGroupCode, :thumbnail, :now, :now)
            ON DUPLICATE KEY UPDATE
                category_group_code = COALESCE(category_group_code, :categoryGroupCode),
                thumbnail_url = COALESCE(:thumbnail, thumbnail_url),
                updated_at = CASE
                    WHEN :thumbnail IS NULL THEN updated_at
                    ELSE :now
                END
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("kakaoId") String kakaoId,
            @Param("name") String name,
            @Param("address") String address,
            @Param("roadAddress") String roadAddress,
            @Param("latitude") java.math.BigDecimal latitude,
            @Param("longitude") java.math.BigDecimal longitude,
            @Param("category") String category,
            @Param("categoryGroupCode") String categoryGroupCode,
            @Param("thumbnail") String thumbnail,
            @Param("now") java.time.Instant now
    );

    @Modifying
    @Query("""
            UPDATE Place place
            SET place.thumbnailUrl = :thumbnailUrl,
                place.updatedAt = :updatedAt
            WHERE place.id = :placeId
            """)
    void updateThumbnail(
            @Param("placeId") Long placeId,
            @Param("thumbnailUrl") String thumbnailUrl,
            @Param("updatedAt") java.time.Instant updatedAt
    );
}
