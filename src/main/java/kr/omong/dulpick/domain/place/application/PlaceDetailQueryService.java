package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType;
import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class PlaceDetailQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceSearchService placeSearchService;
    private final PlaceCategoryWriteThroughService categoryWriteThroughService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PlaceDetailQueryService(
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService
    ) {
        this(placeRepository, placeQueryService, placeSearchService, null, null, Clock.systemUTC());
    }

    @Autowired
    public PlaceDetailQueryService(
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService,
            PlaceCategoryWriteThroughService categoryWriteThroughService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.placeRepository = placeRepository;
        this.placeQueryService = placeQueryService;
        this.placeSearchService = placeSearchService;
        this.categoryWriteThroughService = categoryWriteThroughService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PlaceDetailQueryService(
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService,
            PlaceCategoryWriteThroughService categoryWriteThroughService
    ) {
        this(placeRepository, placeQueryService, placeSearchService,
                categoryWriteThroughService, null, Clock.systemUTC());
    }

    @Transactional(readOnly = true)
    public PlaceDetailView get(Long memberId, Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);
        publishPlaceViewed(memberId, place.getId());
        return toView(memberId, place, null);
    }

    private void publishPlaceViewed(Long memberId, Long placeId) {
        if (eventPublisher == null || memberId == null || placeId == null) {
            return;
        }
        Instant occurredAt = clock.instant();
        String day = occurredAt.atZone(kr.omong.dulpick.global.time.ServiceTime.ZONE_ID)
                .toLocalDate()
                .toString();
        eventPublisher.publishEvent(new AnalyticsActionEvent(
                "PLACE_VIEWED:%d:%d:%s".formatted(memberId, placeId, day),
                AnalyticsEventType.PLACE_VIEWED,
                memberId,
                null,
                "PLACE",
                placeId,
                occurredAt
        ));
    }

    @Transactional(readOnly = true)
    public PlaceDetailView getByKakaoPlaceId(
            Long memberId,
            String kakaoPlaceId,
            String query
    ) {
        PlaceSearchResult kakao = placeSearchService.resolve(query.strip(), kakaoPlaceId);
        Place place = placeRepository.findByKakaoPlaceId(kakaoPlaceId).orElse(null);
        if (place != null) {
            publishPlaceViewed(memberId, place.getId());
        }
        return toView(memberId, place, kakao);
    }

    @Transactional(readOnly = true)
    public List<PlaceDetailView> findByCoordinates(
            Long memberId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        return placeRepository.findAllByLatitudeAndLongitude(
                        scaleCoordinate(latitude),
                        scaleCoordinate(longitude)
                )
                .stream()
                .map(place -> toView(memberId, place, null))
                .toList();
    }

    private BigDecimal scaleCoordinate(BigDecimal value) {
        return value.setScale(7, java.math.RoundingMode.HALF_UP);
    }

    private PlaceDetailView toView(
            Long memberId,
            Place place,
            PlaceSearchResult kakao
    ) {
        Long placeId = place == null ? null : place.getId();
        fillMissingCategory(place, kakao);
        PlaceOwnership ownership = placeId == null
                ? PlaceOwnership.none()
                : placeQueryService.getOwnerships(memberId, List.of(placeId))
                .getOrDefault(placeId, PlaceOwnership.none());
        String categoryGroupCode = firstNonBlank(
                place == null ? null : place.getCategoryGroupCode(),
                kakao == null ? null : kakao.categoryGroupCode()
        );
        String category = firstNonBlank(
                place == null ? null : place.getCategory(),
                kakao == null ? null : kakao.category()
        );
        DulpickPlaceCategory dulpickCategory = place == null
                ? DulpickPlaceCategory.fromKakao(categoryGroupCode, category)
                : place.getDulpickCategory(
                kakao == null ? null : kakao.categoryGroupCode(),
                kakao == null ? null : kakao.category()
        );
        if (dulpickCategory == null) {
            dulpickCategory = DulpickPlaceCategory.fromKakao(categoryGroupCode, category);
        }
        return new PlaceDetailView(
                placeId,
                place == null ? kakao.kakaoPlaceId() : place.getKakaoPlaceId(),
                place == null ? kakao.name() : place.getName(),
                place == null ? kakao.address() : place.getAddress(),
                place == null ? kakao.roadAddress() : place.getRoadAddress(),
                place == null ? kakao.latitude() : place.getLatitude(),
                place == null ? kakao.longitude() : place.getLongitude(),
                category,
                dulpickCategory,
                firstNonBlank(
                        place == null ? null : place.getPhone(),
                        kakao == null ? null : kakao.phone()
                ),
                firstNonBlank(
                        place == null ? null : place.getKakaoPlaceUrl(),
                        kakao == null ? null : kakao.kakaoPlaceUrl()
                ),
                ownership.savedByMe(),
                ownership.status(),
                firstNonBlank(
                        place == null ? null : place.getThumbnailUrl(),
                        kakao == null ? null : kakao.thumbnailUrl()
                ),
                place == null ? List.of() : place.getImageUrls(),
                placeQueryService.savedMemberCount(placeId)
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }

    private void fillMissingCategory(Place place, PlaceSearchResult kakao) {
        if (categoryWriteThroughService == null || place == null) {
            return;
        }
        categoryWriteThroughService.fillIfMissing(
                place.getId(),
                place.getCategoryGroupCode(),
                place.getCategory(),
                kakao == null ? null : kakao.categoryGroupCode(),
                kakao == null ? null : kakao.category(),
                place.getStoredDulpickCategoryCode()
        );
    }
}
