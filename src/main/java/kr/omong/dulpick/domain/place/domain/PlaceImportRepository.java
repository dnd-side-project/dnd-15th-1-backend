package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PlaceImportRepository extends JpaRepository<PlaceImport, Long> {

    Optional<PlaceImport> findByMemberIdAndCanonicalUrlHash(
            Long memberId,
            String canonicalUrlHash
    );

    long countByMemberIdAndCreatedAtGreaterThanEqual(
            Long memberId,
            Instant createdAt
    );
}
