package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberPlaceRepository extends JpaRepository<MemberPlace, Long> {

    Optional<MemberPlace> findByMemberIdAndPlaceId(Long memberId, Long placeId);

    List<MemberPlace> findAllByMemberIdOrderBySavedAtDesc(Long memberId);

    List<MemberPlace> findAllByMemberIdInOrderBySavedAtDesc(List<Long> memberIds);
}
