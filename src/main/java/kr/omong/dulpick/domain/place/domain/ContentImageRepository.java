package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentImageRepository extends JpaRepository<ContentImage, String> {

    List<ContentImage> findAllByContentIdInOrderByContentIdAscDisplayOrderAsc(List<Long> contentIds);

    List<ContentImage> findAllByContentIdInAndContentTypeIsNotNullOrderByContentIdAscDisplayOrderAsc(
            List<Long> contentIds
    );

    Page<ContentImage> findByContentTypeIsNotNull(Pageable pageable);

    List<ContentImage> findAllByContentIdOrderByDisplayOrderAsc(Long contentId);

    Optional<ContentImage> findByContentIdAndSourceUrlHash(Long contentId, String sourceUrlHash);
}
