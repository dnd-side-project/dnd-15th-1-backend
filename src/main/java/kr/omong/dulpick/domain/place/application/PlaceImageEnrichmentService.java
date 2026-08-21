package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentFailureReason;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class PlaceImageEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImageEnrichmentService.class);

    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final PlaceImageProvider imageProvider;
    private final PlaceImageWriter imageWriter;
    private final PlaceImageEnrichmentBacklogRepository backlogRepository;
    private final Clock clock;

    public PlaceImageEnrichmentService(
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImageProvider imageProvider,
            PlaceImageWriter imageWriter,
            PlaceImageEnrichmentBacklogRepository backlogRepository,
            Clock clock
    ) {
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.imageProvider = imageProvider;
        this.imageWriter = imageWriter;
        this.backlogRepository = backlogRepository;
        this.clock = clock;
    }

    public void enrichImportPlaces(Long importId) {
        List<Long> placeIds = candidateRepository.findAllByImportIdOrderByIdAsc(importId)
                .stream()
                .map(candidate -> candidate.getPlaceId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        placeRepository.findAllById(placeIds).stream()
                .filter(this::needsEnrichment)
                .forEach(this::enrich);
    }

    public void enrichPlace(Long placeId) {
        placeRepository.findById(placeId)
                .filter(this::needsEnrichment)
                .ifPresent(this::enrich);
    }

    private boolean needsEnrichment(Place place) {
        return place.getThumbnailUrl() == null || place.getThumbnailUrl().isBlank();
    }

    private void enrich(Place place) {
        String kakaoPlaceId = place.getKakaoPlaceId();
        if (!isValidPlaceId(kakaoPlaceId)) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.INVALID_PLACE_ID);
            return;
        }
        List<String> imageUrls;
        try {
            imageUrls = imageProvider.findImageUrls(kakaoPlaceId);
        } catch (RuntimeException exception) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.PROVIDER_ERROR);
            logger.warn("place_image_enrichment_failed placeId={} reason=PROVIDER_ERROR cause={}",
                    place.getId(), exception.getClass().getSimpleName());
            return;
        }
        if (imageUrls == null || imageUrls.isEmpty()) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.PHOTO_UNAVAILABLE);
            return;
        }
        try {
            imageWriter.replace(place.getId(), imageUrls);
        } catch (RuntimeException exception) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.WRITER_ERROR);
            logger.warn("place_image_enrichment_failed placeId={} reason=WRITER_ERROR cause={}",
                    place.getId(), exception.getClass().getSimpleName());
        }
    }

    private void recordFailure(Place place, PlaceImageEnrichmentFailureReason reason) {
        try {
            backlogRepository.recordFailure(
                    place.getId(),
                    place.getKakaoPlaceId(),
                    reason.name(),
                    Instant.now(clock)
            );
            logger.warn("place_image_enrichment_backlog_recorded placeId={} reason={}",
                    place.getId(), reason);
        } catch (RuntimeException exception) {
            logger.error("place_image_enrichment_backlog_failed placeId={} reason={} cause={}",
                    place.getId(), reason, exception.getClass().getSimpleName());
        }
    }

    private boolean isValidPlaceId(String kakaoPlaceId) {
        return kakaoPlaceId != null && kakaoPlaceId.matches("\\d{1,80}");
    }
}
