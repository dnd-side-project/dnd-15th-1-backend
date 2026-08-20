package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WalkingRouteCacheRepository extends JpaRepository<WalkingRouteCache, Long> {

    Optional<WalkingRouteCache> findByFromPlaceIdAndToPlaceId(Long fromPlaceId, Long toPlaceId);

    List<WalkingRouteCache> findAllByFromPlaceIdInAndToPlaceIdIn(
            Collection<Long> fromPlaceIds,
            Collection<Long> toPlaceIds
    );
}
