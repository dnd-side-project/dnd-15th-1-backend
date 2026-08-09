package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class PlaceImportResultWriter {

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    public PlaceImportResultWriter(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            Clock clock
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    @Transactional
    public void saveSuccess(
            Long importId,
            ContentMetadata metadata,
            List<VerifiedCandidate> verifiedCandidates
    ) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(IllegalStateException::new);
        candidateRepository.deleteAllByImportId(importId);
        List<PlaceCandidate> candidates = verifiedCandidates.stream()
                .map(candidate -> saveCandidate(importId, candidate))
                .toList();
        candidateRepository.saveAll(candidates);
        placeImport.complete(
                metadata.contentHash(),
                metadata.sourceUpdatedAt(),
                clock.instant()
        );
    }

    private PlaceCandidate saveCandidate(
            Long importId,
            VerifiedCandidate candidate
    ) {
        VerifiedPlace verified = candidate.verified();
        Place place = placeRepository.findByKakaoPlaceId(verified.kakaoPlaceId())
                .orElseGet(() -> placeRepository.save(Place.create(
                        verified.kakaoPlaceId(),
                        verified.name(),
                        verified.address(),
                        verified.roadAddress(),
                        verified.latitude(),
                        verified.longitude(),
                        verified.category(),
                        verified.thumbnailUrl(),
                        clock.instant()
                )));
        return PlaceCandidate.verified(
                importId,
                place.getId(),
                candidate.extracted().name(),
                candidate.extracted().addressHint(),
                clock.instant()
        );
    }
}
