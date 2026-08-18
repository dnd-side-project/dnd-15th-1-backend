package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.EmptyPlaceClassificationUpdateException;
import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.ClassificationSource;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationRepository;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdatePlaceClassificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlaceClassificationAdminService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PlaceRepository placeRepository;
    private final PlaceClassificationRepository placeClassificationRepository;
    private final Clock clock;

    public PlaceClassificationAdminService(
            PlaceRepository placeRepository,
            PlaceClassificationRepository placeClassificationRepository,
            Clock clock
    ) {
        this.placeRepository = placeRepository;
        this.placeClassificationRepository = placeClassificationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PlaceClassificationAdminPage list(
            PlaceClassificationStatus status,
            String query,
            Pageable pageable
    ) {
        String keyword = query == null ? "" : query.strip();
        boolean keywordEmpty = keyword.isEmpty();
        Pageable bounded = boundedPage(pageable);
        Page<Place> places = placeRepository.searchForClassificationAdmin(
                keywordEmpty,
                keyword,
                status == null,
                status == PlaceClassificationStatus.UNCLASSIFIED,
                status == PlaceClassificationStatus.PARTIALLY_CLASSIFIED,
                status == PlaceClassificationStatus.CLASSIFIED,
                bounded
        );
        Map<Long, PlaceClassification> classifications = classificationsByPlaceId(places.getContent());
        return new PlaceClassificationAdminPage(
                places.getContent().stream()
                        .map(place -> PlaceClassificationAdminView.from(
                                place,
                                classifications.get(place.getId())
                        ))
                        .toList(),
                places.getNumber(),
                places.getSize(),
                places.getTotalElements(),
                places.getTotalPages(),
                places.hasNext(),
                new PlaceClassificationAdminPage.StatusCounts(
                        count(keywordEmpty, keyword, null),
                        count(keywordEmpty, keyword, PlaceClassificationStatus.UNCLASSIFIED),
                        count(keywordEmpty, keyword, PlaceClassificationStatus.PARTIALLY_CLASSIFIED),
                        count(keywordEmpty, keyword, PlaceClassificationStatus.CLASSIFIED)
                )
        );
    }

    @Transactional(readOnly = true)
    public PlaceClassificationAdminView get(Long placeId) {
        Place place = placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
        PlaceClassification classification = placeClassificationRepository.findById(placeId)
                .orElse(null);
        return PlaceClassificationAdminView.from(place, classification);
    }

    @Transactional
    public PlaceClassificationAdminView update(Long placeId, UpdatePlaceClassificationRequest request) {
        if (request == null || !request.hasAnyChange()) {
            throw new EmptyPlaceClassificationUpdateException();
        }
        Place place = placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
        Instant now = clock.instant();
        PlaceClassification classification = placeClassificationRepository.findById(placeId)
                .orElseGet(() -> PlaceClassification.initialize(placeId, now));
        apply(classification, request, now);
        placeClassificationRepository.save(classification);
        return PlaceClassificationAdminView.from(place, classification);
    }

    private void apply(
            PlaceClassification classification,
            UpdatePlaceClassificationRequest request,
            Instant now
    ) {
        if (request.isEnvironmentPresent()) {
            if (request.getEnvironment() == null) {
                classification.clearEnvironment(now);
            } else {
                classification.classifyEnvironment(
                        request.getEnvironment(),
                        ClassificationSource.MANUAL,
                        now
                );
            }
        }
        if (request.isActivityPresent()) {
            if (request.getActivity() == null) {
                classification.clearActivity(now);
            } else {
                classification.classifyActivity(
                        request.getActivity(),
                        ClassificationSource.MANUAL,
                        now
                );
            }
        }
        if (request.isTimePresent()) {
            if (request.getTime() == null) {
                classification.clearTime(now);
            } else {
                classification.classifyTime(request.getTime(), ClassificationSource.MANUAL, now);
            }
        }
        if (request.isFocusPresent()) {
            if (request.getFocus() == null) {
                classification.clearFocus(now);
            } else {
                classification.classifyFocus(request.getFocus(), ClassificationSource.MANUAL, now);
            }
        }
    }

    private Map<Long, PlaceClassification> classificationsByPlaceId(List<Place> places) {
        List<Long> placeIds = places.stream().map(Place::getId).toList();
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return placeClassificationRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(PlaceClassification::getPlaceId, Function.identity()));
    }

    private long count(
            boolean keywordEmpty,
            String keyword,
            PlaceClassificationStatus status
    ) {
        return placeRepository.searchForClassificationAdmin(
                keywordEmpty,
                keyword,
                status == null,
                status == PlaceClassificationStatus.UNCLASSIFIED,
                status == PlaceClassificationStatus.PARTIALLY_CLASSIFIED,
                status == PlaceClassificationStatus.CLASSIFIED,
                PageRequest.of(0, 1)
        ).getTotalElements();
    }

    private Pageable boundedPage(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size);
    }
}
