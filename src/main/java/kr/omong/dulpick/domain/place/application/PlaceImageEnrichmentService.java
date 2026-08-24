package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklog;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentFailureReason;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.config.PlaceImageEnrichmentProperties;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PlaceImageEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImageEnrichmentService.class);

    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final PlaceImageProvider imageProvider;
    private final PlaceImageWriter imageWriter;
    private final PlaceImageStorageService imageStorageService;
    private final PlaceImageEnrichmentBacklogRepository backlogRepository;
    private final PlaceImageEnrichmentProperties properties;
    private final Clock clock;

    public PlaceImageEnrichmentService(
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImageProvider imageProvider,
            PlaceImageWriter imageWriter,
            PlaceImageStorageService imageStorageService,
            PlaceImageEnrichmentBacklogRepository backlogRepository,
            PlaceImageEnrichmentProperties properties,
            Clock clock
    ) {
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.imageProvider = imageProvider;
        this.imageWriter = imageWriter;
        this.imageStorageService = imageStorageService;
        this.backlogRepository = backlogRepository;
        this.properties = properties;
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
        return place.getThumbnailUrl() == null
                || place.getThumbnailUrl().isBlank()
                || !imageStorageService.isPublicUrl(place.getThumbnailUrl());
    }

    private void enrich(Place place) {
        if (!canAttempt(place.getId())) {
            logger.info("place_image_enrichment_skipped placeId={} reason=RETRY_LIMIT_OR_COOLDOWN",
                    place.getId());
            return;
        }
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
            backlogRepository.deleteByPlaceId(place.getId());
        } catch (RuntimeException exception) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.WRITER_ERROR);
            logger.warn("place_image_enrichment_failed placeId={} reason=WRITER_ERROR cause={}",
                    place.getId(), exception.getClass().getSimpleName());
        }
    }

    private boolean canAttempt(Long placeId) {
        Optional<PlaceImageEnrichmentBacklog> backlog = backlogRepository.findByPlaceId(placeId);
        if (backlog.isEmpty()) {
            return true;
        }
        PlaceImageEnrichmentBacklog failed = backlog.get();
        if (!"PENDING".equals(failed.getStatus())
                || failed.getAttemptCount() >= properties.maxAttempts()) {
            return false;
        }
        return !failed.getLastFailedAt()
                .plus(properties.retryCooldown())
                .isAfter(clock.instant());
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
