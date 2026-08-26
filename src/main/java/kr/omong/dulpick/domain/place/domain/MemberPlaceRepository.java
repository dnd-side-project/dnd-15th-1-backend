package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
