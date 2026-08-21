package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

    List<Place> findAllByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);

    List<Place> findAllByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);

    @Query(
            value = """
                    SELECT *
                    FROM places
                    WHERE MATCH(name, address, road_address) AGAINST(:query IN BOOLEAN MODE)
                    ORDER BY id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM places
                    WHERE MATCH(name, address, road_address) AGAINST(:query IN BOOLEAN MODE)
                    """,
            nativeQuery = true
    )
    Page<Place> searchByKeyword(@Param("query") String query, Pageable pageable);

    @Query(
            value = """
                    SELECT place
                    FROM Place place
                    LEFT JOIN PlaceClassification classification ON classification.placeId = place.id
                    WHERE EXISTS (
                        SELECT 1
                        FROM ContentPlace contentPlace
                        WHERE contentPlace.placeId = place.id
                    )
                    AND (:keywordEmpty = true
                        OR LOWER(place.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(place.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(place.roadAddress, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (
                        :matchAll = true
                        OR (:matchUnclassified = true
                            AND (classification.placeId IS NULL
                                OR (classification.environment IS NULL
                                    AND classification.activity IS NULL
                                    AND classification.time IS NULL
                                    AND classification.focus IS NULL)))
                        OR (:matchClassified = true
                            AND classification.environment IS NOT NULL
                            AND classification.activity IS NOT NULL
                            AND classification.time IS NOT NULL
                            AND classification.focus IS NOT NULL)
                        OR (:matchPartial = true
                            AND classification.placeId IS NOT NULL
                            AND NOT (classification.environment IS NULL
                                AND classification.activity IS NULL
                                AND classification.time IS NULL
                                AND classification.focus IS NULL)
                            AND NOT (classification.environment IS NOT NULL
                                AND classification.activity IS NOT NULL
                                AND classification.time IS NOT NULL
                                AND classification.focus IS NOT NULL))
                    )
                    ORDER BY
                        CASE
                            WHEN classification.placeId IS NULL
                                OR (classification.environment IS NULL
                                    AND classification.activity IS NULL
                                    AND classification.time IS NULL
                                    AND classification.focus IS NULL)
                            THEN 0
                            WHEN classification.environment IS NOT NULL
                                AND classification.activity IS NOT NULL
                                AND classification.time IS NOT NULL
                                AND classification.focus IS NOT NULL
                            THEN 2
                            ELSE 1
                        END,
                        place.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(place)
                    FROM Place place
                    LEFT JOIN PlaceClassification classification ON classification.placeId = place.id
                    WHERE EXISTS (
                        SELECT 1
                        FROM ContentPlace contentPlace
                        WHERE contentPlace.placeId = place.id
                    )
                    AND (:keywordEmpty = true
                        OR LOWER(place.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(place.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(place.roadAddress, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    AND (
                        :matchAll = true
                        OR (:matchUnclassified = true
                            AND (classification.placeId IS NULL
                                OR (classification.environment IS NULL
                                    AND classification.activity IS NULL
                                    AND classification.time IS NULL
                                    AND classification.focus IS NULL)))
                        OR (:matchClassified = true
                            AND classification.environment IS NOT NULL
                            AND classification.activity IS NOT NULL
                            AND classification.time IS NOT NULL
                            AND classification.focus IS NOT NULL)
                        OR (:matchPartial = true
                            AND classification.placeId IS NOT NULL
                            AND NOT (classification.environment IS NULL
                                AND classification.activity IS NULL
                                AND classification.time IS NULL
                                AND classification.focus IS NULL)
                            AND NOT (classification.environment IS NOT NULL
                                AND classification.activity IS NOT NULL
                                AND classification.time IS NOT NULL
                                AND classification.focus IS NOT NULL))
                    )
                    """
    )
    Page<Place> searchForClassificationAdmin(
            @Param("keywordEmpty") boolean keywordEmpty,
            @Param("keyword") String keyword,
            @Param("matchAll") boolean matchAll,
            @Param("matchUnclassified") boolean matchUnclassified,
            @Param("matchPartial") boolean matchPartial,
            @Param("matchClassified") boolean matchClassified,
            Pageable pageable
    );

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
                 category, category_group_code, phone, kakao_place_url,
                 thumbnail_url, created_at, updated_at)
            VALUES
                (:kakaoId, :name, :address, NULLIF(TRIM(:roadAddress), ''), :latitude, :longitude,
                 :category, :categoryGroupCode, :phone, :kakaoPlaceUrl,
                 :thumbnail, :now, :now)
            ON DUPLICATE KEY UPDATE
                road_address = COALESCE(
                    NULLIF(TRIM(road_address), ''),
                    NULLIF(TRIM(:roadAddress), '')
                ),
                category_group_code = COALESCE(
                    NULLIF(TRIM(category_group_code), ''),
                    NULLIF(TRIM(:categoryGroupCode), '')
                ),
                phone = COALESCE(NULLIF(:phone, ''), phone),
                kakao_place_url = COALESCE(NULLIF(:kakaoPlaceUrl, ''), kakao_place_url),
                thumbnail_url = COALESCE(:thumbnail, thumbnail_url),
                updated_at = CASE
                    WHEN :thumbnail IS NULL
                         AND NULLIF(TRIM(:categoryGroupCode), '') IS NULL
                         AND (:phone IS NULL OR :phone = '')
                         AND (:kakaoPlaceUrl IS NULL OR :kakaoPlaceUrl = '')
                    THEN updated_at
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
            @Param("phone") String phone,
            @Param("kakaoPlaceUrl") String kakaoPlaceUrl,
            @Param("thumbnail") String thumbnail,
            @Param("now") Instant now
    );

    default void insertIfAbsent(
            String kakaoId,
            String name,
            String address,
            String roadAddress,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            String category,
            String categoryGroupCode,
            String thumbnail,
            Instant now
    ) {
        insertIfAbsent(
                kakaoId,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                category,
                categoryGroupCode,
                null,
                null,
                thumbnail,
                now
        );
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Place place
            SET place.thumbnailUrl = :thumbnailUrl,
                place.updatedAt = :updatedAt
            WHERE place.id = :placeId
            """)
    void updateThumbnail(
            @Param("placeId") Long placeId,
            @Param("thumbnailUrl") String thumbnailUrl,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Place place
            SET place.categoryGroupCode = COALESCE(
                        NULLIF(TRIM(place.categoryGroupCode), ''),
                        NULLIF(TRIM(:categoryGroupCode), '')
                    ),
                place.category = COALESCE(
                        NULLIF(TRIM(place.category), ''),
                        NULLIF(TRIM(:category), '')
                    ),
                place.updatedAt = :updatedAt
            WHERE place.id = :placeId
              AND (
                    place.categoryGroupCode IS NULL
                    OR TRIM(place.categoryGroupCode) = ''
                    OR place.category IS NULL
                    OR TRIM(place.category) = ''
                  )
            """)
    int updateCategoryIfMissing(
            @Param("placeId") Long placeId,
            @Param("categoryGroupCode") String categoryGroupCode,
            @Param("category") String category,
            @Param("updatedAt") Instant updatedAt
    );
}
