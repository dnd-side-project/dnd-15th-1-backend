package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

    @Modifying
    @Query(value = """
            INSERT INTO places
                (kakao_place_id, name, address, road_address, latitude, longitude,
                 category, category_group_code, thumbnail_url, created_at, updated_at)
            VALUES
                (:kakaoId, :name, :address, :roadAddress, :latitude, :longitude,
                 :category, :categoryGroupCode, :thumbnail, :now, :now)
            ON DUPLICATE KEY UPDATE
                category_group_code = COALESCE(category_group_code, :categoryGroupCode)
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
}
