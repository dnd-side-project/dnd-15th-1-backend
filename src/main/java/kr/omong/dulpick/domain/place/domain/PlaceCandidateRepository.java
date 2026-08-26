package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceCandidateRepository extends JpaRepository<PlaceCandidate, Long> {

    Optional<PlaceCandidate> findByIdAndImportId(Long id, Long importId);

    List<PlaceCandidate> findAllByImportIdOrderByIdAsc(Long importId);

    void deleteAllByImportId(Long importId);
}
