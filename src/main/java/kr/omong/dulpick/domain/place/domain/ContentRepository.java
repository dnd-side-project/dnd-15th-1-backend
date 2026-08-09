package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findByCanonicalUrlHash(String canonicalUrlHash);

    List<Content> findAllByPublicationStatusOrderByCreatedAtDesc(ContentPublicationStatus status);
}
