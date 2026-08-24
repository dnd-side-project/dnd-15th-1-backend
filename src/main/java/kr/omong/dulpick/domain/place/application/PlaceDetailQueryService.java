package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlaceDetailQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceSearchService placeSearchService;
    private final PlaceCategoryWriteThroughService categoryWriteThroughService;

    public PlaceDetailQueryService(
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService
    ) {
        this(placeRepository, placeQueryService, placeSearchService, null);
    }

    @Autowired
    public PlaceDetailQueryService(
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService,
            PlaceCategoryWriteThroughService categoryWriteThroughService
    ) {
        this.placeRepository = placeRepository;
        this.placeQueryService = placeQueryService;
        this.placeSearchService = placeSearchService;
        this.categoryWriteThroughService = categoryWriteThroughService;
    }

    @Transactional(readOnly = true)
    public PlaceDetailView get(Long memberId, Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);
        return toView(memberId, place, null);
    }

    @Transactional(readOnly = true)
    public PlaceDetailView getByKakaoPlaceId(
            Long memberId,
            String kakaoPlaceId,
            String query
    ) {
        PlaceSearchResult kakao = placeSearchService.resolve(query.strip(), kakaoPlaceId);
        Place place = placeRepository.findByKakaoPlaceId(kakaoPlaceId).orElse(null);
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
        return new PlaceDetailView(
                placeId,
                place == null ? kakao.kakaoPlaceId() : place.getKakaoPlaceId(),
                place == null ? kakao.name() : place.getName(),
                place == null ? kakao.address() : place.getAddress(),
                place == null ? kakao.roadAddress() : place.getRoadAddress(),
                place == null ? kakao.latitude() : place.getLatitude(),
                place == null ? kakao.longitude() : place.getLongitude(),
                category,
                place == null
                        ? DulpickPlaceCategory.fromKakao(categoryGroupCode, category)
                        : place.getDulpickCategory(),
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
        if (categoryWriteThroughService == null || place == null || kakao == null) {
            return;
        }
        categoryWriteThroughService.fillIfMissing(
                place.getId(),
                place.getCategoryGroupCode(),
                place.getCategory(),
                kakao.categoryGroupCode(),
                kakao.category()
        );
    }
}
