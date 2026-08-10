package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class PlaceImageEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImageEnrichmentService.class);

    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final PlaceImageProvider imageProvider;
    private final PlaceImageWriter imageWriter;

    public PlaceImageEnrichmentService(
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImageProvider imageProvider,
            PlaceImageWriter imageWriter
    ) {
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.imageProvider = imageProvider;
        this.imageWriter = imageWriter;
    }

    public void enrichImportPlaces(Long importId) {
        List<Long> placeIds = candidateRepository.findAllByImportIdOrderByIdAsc(importId)
                .stream()
                .map(candidate -> candidate.getPlaceId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        placeRepository.findAllById(placeIds).forEach(this::enrich);
    }

    private void enrich(Place place) {
        try {
            List<String> imageUrls = imageProvider.findImageUrls(place.getKakaoPlaceId());
            imageWriter.replace(place.getId(), imageUrls);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Place image enrichment failed: placeId={}, cause={}",
                    place.getId(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
