package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentPlaceRepository extends JpaRepository<ContentPlace, Long> {

    boolean existsByContentIdAndPlaceId(Long contentId, Long placeId);

    List<ContentPlace> findAllByContentId(Long contentId);

    List<ContentPlace> findAllByContentIdIn(List<Long> contentIds);

    void deleteAllByContentId(Long contentId);
}
