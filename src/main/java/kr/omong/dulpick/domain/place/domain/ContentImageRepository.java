package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentImageRepository extends JpaRepository<ContentImage, String> {

    List<ContentImage> findAllByContentIdInOrderByContentIdAscDisplayOrderAsc(List<Long> contentIds);

    List<ContentImage> findAllByContentIdInAndContentTypeIsNotNullOrderByContentIdAscDisplayOrderAsc(
            List<Long> contentIds
    );

    List<ContentImage> findAllByContentIdOrderByDisplayOrderAsc(Long contentId);

    Optional<ContentImage> findByContentIdAndSourceUrlHash(Long contentId, String sourceUrlHash);
}
