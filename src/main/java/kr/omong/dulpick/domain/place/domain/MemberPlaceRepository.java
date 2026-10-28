package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberPlaceRepository extends JpaRepository<MemberPlace, Long> {

    Optional<MemberPlace> findByMemberIdAndPlaceId(Long memberId, Long placeId);

    List<MemberPlace> findAllByMemberIdAndPlaceIdIn(Long memberId, List<Long> placeIds);

    @Query("""
            SELECT mp.place.id, COUNT(mp)
            FROM MemberPlace mp
            WHERE mp.place.id IN :placeIds
            GROUP BY mp.place.id
            """)
    List<Object[]> countSavesByPlaceIdIn(@Param("placeIds") Collection<Long> placeIds);

    List<MemberPlace> findAllByMemberIdOrderBySavedAtDesc(Long memberId);

    @EntityGraph(attributePaths = "place")
    List<MemberPlace> findAllByMemberIdInOrderBySavedAtDesc(List<Long> memberIds);

    @EntityGraph(attributePaths = "place")
    List<MemberPlace> findAllByMemberIdInAndPlaceIdIn(
            List<Long> memberIds,
            List<Long> placeIds
    );

    @Query(value = """
            SELECT place.id
            FROM places place
            JOIN member_places member_place
              ON member_place.place_id = place.id
            LEFT JOIN place_classifications classification
              ON classification.place_id = place.id
            WHERE member_place.member_id IN (:memberIds)
              AND (
                    :keywordEmpty = true
                    OR REPLACE(LOWER(place.name), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
                    OR REPLACE(LOWER(COALESCE(place.address, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
                    OR REPLACE(LOWER(COALESCE(place.road_address, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
                    OR REPLACE(LOWER(COALESCE(member_place.alias, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
              )
              AND (:environmentType IS NULL
                   OR classification.environment_type = :environmentType)
              AND (:activityType IS NULL
                   OR classification.activity_type = :activityType)
              AND (:timeType IS NULL
                   OR classification.time_type = :timeType)
              AND (:focusType IS NULL
                   OR classification.focus_type = :focusType)
              AND (:categoryCode IS NULL
                   OR place.dulpick_category_code = :categoryCode
                   OR (place.dulpick_category_code IS NULL
                       AND place.category_group_code IN (:categoryGroupCodes)))
            GROUP BY place.id
            ORDER BY MIN(CASE
                WHEN :keywordEmpty = false
                    AND REPLACE(LOWER(place.name), ' ', '') = :keyword THEN 0
                WHEN :keywordEmpty = false
                    AND REPLACE(LOWER(place.name), ' ', '') LIKE CONCAT(:keyword, '%') THEN 1
                WHEN :keywordEmpty = false
                    AND REPLACE(LOWER(place.name), ' ', '') LIKE CONCAT('%', :keyword, '%') THEN 2
                WHEN :keywordEmpty = false
                    AND REPLACE(LOWER(COALESCE(member_place.alias, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%') THEN 3
                WHEN :keywordEmpty = false
                    AND (
                        REPLACE(LOWER(COALESCE(place.address, '')), ' ', '')
                            LIKE CONCAT('%', :keyword, '%')
                        OR REPLACE(LOWER(COALESCE(place.road_address, '')), ' ', '')
                            LIKE CONCAT('%', :keyword, '%')
                    ) THEN 4
                ELSE 5
            END), MAX(member_place.saved_at) DESC, place.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT place.id)
            FROM places place
            JOIN member_places member_place
              ON member_place.place_id = place.id
            LEFT JOIN place_classifications classification
              ON classification.place_id = place.id
            WHERE member_place.member_id IN (:memberIds)
              AND (
                    :keywordEmpty = true
                    OR REPLACE(LOWER(place.name), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
                    OR REPLACE(LOWER(COALESCE(place.address, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
                    OR REPLACE(LOWER(COALESCE(place.road_address, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
                    OR REPLACE(LOWER(COALESCE(member_place.alias, '')), ' ', '')
                        LIKE CONCAT('%', :keyword, '%')
              )
              AND (:environmentType IS NULL
                   OR classification.environment_type = :environmentType)
              AND (:activityType IS NULL
                   OR classification.activity_type = :activityType)
              AND (:timeType IS NULL
                   OR classification.time_type = :timeType)
              AND (:focusType IS NULL
                   OR classification.focus_type = :focusType)
              AND (:categoryCode IS NULL
                   OR place.dulpick_category_code = :categoryCode
                   OR (place.dulpick_category_code IS NULL
                       AND place.category_group_code IN (:categoryGroupCodes)))
            """,
            nativeQuery = true)
    Page<Long> searchVisiblePlaceIds(
            @Param("memberIds") List<Long> memberIds,
            @Param("keyword") String keyword,
            @Param("keywordEmpty") boolean keywordEmpty,
            @Param("environmentType") String environmentType,
            @Param("activityType") String activityType,
            @Param("timeType") String timeType,
            @Param("focusType") String focusType,
            @Param("categoryCode") String categoryCode,
            @Param("categoryGroupCodes") List<String> categoryGroupCodes,
            Pageable pageable
    );
}
