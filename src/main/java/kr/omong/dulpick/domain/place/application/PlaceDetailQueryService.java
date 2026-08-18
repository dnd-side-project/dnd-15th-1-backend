package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PlaceDetailQueryService {

    private final PlaceRepository placeRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceSearchService placeSearchService;
    private final RegionTagQueryService regionTagQueryService;

    public PlaceDetailQueryService(
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService,
            RegionTagQueryService regionTagQueryService
    ) {
        this.placeRepository = placeRepository;
        this.placeQueryService = placeQueryService;
        this.placeSearchService = placeSearchService;
        this.regionTagQueryService = regionTagQueryService;
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

    private PlaceDetailView toView(
            Long memberId,
            Place place,
            PlaceSearchResult kakao
    ) {
        Long placeId = place == null ? null : place.getId();
        PlaceOwnership ownership = placeId == null
                ? PlaceOwnership.none()
                : placeQueryService.getOwnerships(memberId, List.of(placeId))
                .getOrDefault(placeId, PlaceOwnership.none());
        String categoryGroupCode = place == null
                ? kakao.categoryGroupCode()
                : place.getCategoryGroupCode();
        String category = place == null ? kakao.category() : place.getCategory();
        List<RegionTagSummaryView> regionTags = place == null
                ? regionTagQueryService.matchingTags(
                kakao.address(),
                kakao.roadAddress(),
                regionTagQueryService.getActiveSummaries()
        )
                : regionTagQueryService.getTagsByPlaceIds(List.of(placeId))
                .getOrDefault(placeId, List.of());
        return new PlaceDetailView(
                placeId,
                place == null ? kakao.kakaoPlaceId() : place.getKakaoPlaceId(),
                place == null ? kakao.name() : place.getName(),
                place == null ? kakao.address() : place.getAddress(),
                place == null ? kakao.roadAddress() : place.getRoadAddress(),
                place == null ? kakao.latitude() : place.getLatitude(),
                place == null ? kakao.longitude() : place.getLongitude(),
                category,
                DulpickPlaceCategory.fromKakao(categoryGroupCode, category),
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
                regionTags
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
