package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklog;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentFailureReason;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.config.PlaceImageEnrichmentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

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
    private final Executor recoveryExecutor;
    private final Set<Long> inFlightPlaces = ConcurrentHashMap.newKeySet();

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
        this(
                candidateRepository,
                placeRepository,
                imageProvider,
                imageWriter,
                imageStorageService,
                backlogRepository,
                properties,
                clock,
                Runnable::run
        );
    }

    @Autowired
    public PlaceImageEnrichmentService(
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImageProvider imageProvider,
            PlaceImageWriter imageWriter,
            PlaceImageStorageService imageStorageService,
            PlaceImageEnrichmentBacklogRepository backlogRepository,
            PlaceImageEnrichmentProperties properties,
            Clock clock,
            @Qualifier("placeImageExecutor") Executor recoveryExecutor
    ) {
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.imageProvider = imageProvider;
        this.imageWriter = imageWriter;
        this.imageStorageService = imageStorageService;
        this.backlogRepository = backlogRepository;
        this.properties = properties;
        this.clock = clock;
        this.recoveryExecutor = recoveryExecutor;
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
                .forEach(this::enrichWithLock);
    }

    public boolean enrichPlace(Long placeId) {
        if (!inFlightPlaces.add(placeId)) {
            return true;
        }
        try {
            return placeRepository.findById(placeId)
                    .filter(this::needsEnrichment)
                    .map(this::enrich)
                    .orElse(true);
        } finally {
            inFlightPlaces.remove(placeId);
        }
    }

    private boolean enrichWithLock(Place place) {
        if (!inFlightPlaces.add(place.getId())) {
            return true;
        }
        try {
            return enrich(place);
        } finally {
            inFlightPlaces.remove(place.getId());
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void recoverPending() {
        backlogRepository.findByStatusAndLastFailedAtBeforeOrderByLastFailedAtAsc(
                        "PENDING",
                        clock.instant().minus(properties.retryCooldown()),
                        PageRequest.of(0, 20)
                )
                .forEach(backlog -> submitRecovery(backlog.getPlaceId()));
    }

    private void submitRecovery(Long placeId) {
        try {
            recoveryExecutor.execute(() -> enrichPlace(placeId));
        } catch (RejectedExecutionException exception) {
            logger.warn(
                    "Place image recovery dispatch rejected: placeId={}, cause={}",
                    placeId,
                    exception.getClass().getSimpleName()
            );
        }
    }

    public void recordImportDispatchFailure(Long importId) {
        List<Long> placeIds = candidateRepository.findAllByImportIdOrderByIdAsc(importId)
                .stream()
                .map(candidate -> candidate.getPlaceId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        placeRepository.findAllById(placeIds)
                .forEach(place -> recordFailure(place, PlaceImageEnrichmentFailureReason.DISPATCH_REJECTED));
    }

    public void recordPlaceDispatchFailure(Long placeId) {
        placeRepository.findById(placeId)
                .ifPresent(place -> recordFailure(place, PlaceImageEnrichmentFailureReason.DISPATCH_REJECTED));
    }

    private boolean needsEnrichment(Place place) {
        return place.getThumbnailUrl() == null
                || place.getThumbnailUrl().isBlank()
                || !imageStorageService.isPublicUrl(place.getThumbnailUrl());
    }

    private boolean enrich(Place place) {
        if (!canAttempt(place.getId())) {
            logger.info("place_image_enrichment_skipped placeId={} reason=RETRY_LIMIT_OR_COOLDOWN",
                    place.getId());
            return false;
        }
        String kakaoPlaceId = place.getKakaoPlaceId();
        if (!isValidPlaceId(kakaoPlaceId)) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.INVALID_PLACE_ID);
            return false;
        }
        List<String> imageUrls;
        try {
            imageUrls = imageProvider.findImageUrls(kakaoPlaceId);
        } catch (RuntimeException exception) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.PROVIDER_ERROR);
            logger.warn("place_image_enrichment_failed placeId={} reason=PROVIDER_ERROR cause={}",
                    place.getId(), exception.getClass().getSimpleName());
            return false;
        }
        if (imageUrls == null || imageUrls.isEmpty()) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.PHOTO_UNAVAILABLE);
            return false;
        }
        try {
            if (!imageWriter.replace(place.getId(), imageUrls)) {
                recordFailure(place, PlaceImageEnrichmentFailureReason.WRITER_ERROR);
                return false;
            }
            backlogRepository.deleteByPlaceId(place.getId());
            return true;
        } catch (RuntimeException exception) {
            recordFailure(place, PlaceImageEnrichmentFailureReason.WRITER_ERROR);
            logger.warn("place_image_enrichment_failed placeId={} reason=WRITER_ERROR cause={}",
                    place.getId(), exception.getClass().getSimpleName());
            return false;
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
