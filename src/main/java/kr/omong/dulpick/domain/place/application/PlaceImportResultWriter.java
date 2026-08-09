package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSubmission;
import kr.omong.dulpick.domain.place.domain.ContentSubmissionRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class PlaceImportResultWriter {

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final ContentRepository contentRepository;
    private final ContentPlaceRepository contentPlaceRepository;
    private final ContentSubmissionRepository submissionRepository;
    private final Clock clock;

    public PlaceImportResultWriter(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            ContentRepository contentRepository,
            ContentPlaceRepository contentPlaceRepository,
            ContentSubmissionRepository submissionRepository,
            Clock clock
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.contentRepository = contentRepository;
        this.contentPlaceRepository = contentPlaceRepository;
        this.submissionRepository = submissionRepository;
        this.clock = clock;
    }

    @Transactional
    public Long saveMetadata(Long importId, ContentMetadata metadata) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(IllegalStateException::new);
        Content content = findOrCreateContent(metadata);
        placeImport.attachContent(content.getId());
        if (!submissionRepository.existsByContentIdAndMemberId(content.getId(), placeImport.getMemberId())) {
            submissionRepository.save(ContentSubmission.create(
                    content.getId(),
                    placeImport.getMemberId(),
                    clock.instant()
            ));
        }
        placeImport.recordMetadata(
                metadata.title(),
                metadata.caption(),
                metadata.thumbnailUrl(),
                metadata.contentHash(),
                metadata.sourceUpdatedAt()
        );
        return content.getId();
    }

    @Transactional
    public void saveSuccess(
            Long importId,
            ContentMetadata metadata,
            List<VerifiedCandidate> verifiedCandidates
    ) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(IllegalStateException::new);
        Long contentId = placeImport.getContentId();
        if (contentId == null) {
            contentId = findOrCreateContent(metadata).getId();
            placeImport.attachContent(contentId);
        }
        final Long resolvedContentId = contentId;
        candidateRepository.deleteAllByImportId(importId);
        List<PlaceCandidate> candidates = verifiedCandidates.stream()
                .map(candidate -> saveCandidate(importId, resolvedContentId, candidate))
                .toList();
        candidateRepository.saveAll(candidates);
        placeImport.complete(
                metadata.title(),
                metadata.caption(),
                metadata.thumbnailUrl(),
                metadata.contentHash(),
                metadata.sourceUpdatedAt(),
                clock.instant()
        );
    }

    private PlaceCandidate saveCandidate(
            Long importId,
            Long contentId,
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
        if (!contentPlaceRepository.existsByContentIdAndPlaceId(contentId, place.getId())) {
            contentPlaceRepository.save(ContentPlace.create(
                    contentId,
                    place.getId(),
                    clock.instant()
            ));
        }
        return PlaceCandidate.verified(
                importId,
                place.getId(),
                candidate.extracted().name(),
                candidate.extracted().addressHint(),
                clock.instant()
        );
    }

    private Content findOrCreateContent(ContentMetadata metadata) {
        String urlHash = Sha256.hex(metadata.canonicalUrl());
        Content content = contentRepository.findByCanonicalUrlHash(urlHash)
                .orElseGet(() -> contentRepository.save(Content.create(
                        metadata.canonicalUrl(),
                        urlHash,
                        metadata.sourceType(),
                        metadata.title(),
                        metadata.caption(),
                        metadata.thumbnailUrl(),
                        clock.instant()
                )));
        content.updateMetadata(
                metadata.title(),
                metadata.caption(),
                metadata.thumbnailUrl(),
                clock.instant()
        );
        return content;
    }
}
