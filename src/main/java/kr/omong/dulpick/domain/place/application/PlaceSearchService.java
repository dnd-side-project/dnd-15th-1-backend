package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.search.FullTextBooleanQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlaceSearchService {

    static final int PAGE_SIZE = 10;

    private final PlaceSearcher placeSearcher;
    private final PlaceRepository placeRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceCategoryWriteThroughService categoryWriteThroughService;

    public PlaceSearchService(
            PlaceSearcher placeSearcher,
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService
    ) {
        this(placeSearcher, placeRepository, placeQueryService, null);
    }

    @Autowired
    public PlaceSearchService(
            PlaceSearcher placeSearcher,
            PlaceRepository placeRepository,
            PlaceQueryService placeQueryService,
            PlaceCategoryWriteThroughService categoryWriteThroughService
    ) {
        this.placeSearcher = placeSearcher;
        this.placeRepository = placeRepository;
        this.placeQueryService = placeQueryService;
        this.categoryWriteThroughService = categoryWriteThroughService;
    }

    public List<PlaceSearchResult> search(String query) {
        return placeSearcher.search(query);
    }

    @Transactional(readOnly = true)
    public PlaceSearchPage search(
            Long memberId,
            String query,
            int page
    ) {
        List<Place> databaseResults = page == 0
                ? placeRepository.searchByKeyword(
                FullTextBooleanQuery.from(query.strip()),
                PageRequest.of(0, PAGE_SIZE)
        ).getContent()
                : List.of();
        PlaceKeywordSearch kakaoSearch = placeSearcher.search(
                query.strip(),
                page + PlaceSearcher.FIRST_PAGE
        );
        List<PlaceSearchResult> kakaoResults = kakaoSearch.results();
        Map<String, PlaceSearchResult> kakaoById = kakaoResults.stream()
                .collect(Collectors.toMap(
                        PlaceSearchResult::kakaoPlaceId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        Map<String, Place> placesByKakaoId = findPlacesByKakaoId(
                databaseResults,
                kakaoResults
        );
        LinkedHashMap<String, SearchCandidate> candidates = new LinkedHashMap<>();
        databaseResults.forEach(place -> candidates.put(
                place.getKakaoPlaceId(),
                new SearchCandidate(place, kakaoById.get(place.getKakaoPlaceId()))
        ));
        kakaoResults.forEach(result -> candidates.putIfAbsent(
                result.kakaoPlaceId(),
                new SearchCandidate(placesByKakaoId.get(result.kakaoPlaceId()), result)
        ));

        List<Long> placeIds = candidates.values().stream()
                .map(SearchCandidate::place)
                .filter(java.util.Objects::nonNull)
                .map(Place::getId)
                .toList();
        Map<Long, PlaceOwnership> ownerships =
                placeQueryService.getOwnerships(memberId, placeIds);

        List<PlaceSearchView> places = candidates.values().stream()
                .map(candidate -> toView(candidate, ownerships))
                .map(PlaceSearchViewWithOwnership::view)
                .limit(PAGE_SIZE)
                .toList();
        return new PlaceSearchPage(places, page, PAGE_SIZE, !kakaoSearch.lastPage());
    }

    public PlaceSearchResult resolve(String query, String kakaoPlaceId) {
        return search(query).stream()
                .filter(result -> result.kakaoPlaceId().equals(kakaoPlaceId))
                .findFirst()
                .orElseThrow(PlaceNotFoundException::new);
    }

    private Map<String, Place> findPlacesByKakaoId(
            List<Place> databaseResults,
            List<PlaceSearchResult> kakaoResults
    ) {
        List<String> kakaoPlaceIds = java.util.stream.Stream.concat(
                        databaseResults.stream().map(Place::getKakaoPlaceId),
                        kakaoResults.stream().map(PlaceSearchResult::kakaoPlaceId)
                )
                .distinct()
                .toList();
        if (kakaoPlaceIds.isEmpty()) {
            return Map.of();
        }
        return placeRepository.findAllByKakaoPlaceIdIn(kakaoPlaceIds)
                .stream()
                .collect(Collectors.toMap(Place::getKakaoPlaceId, Function.identity()));
    }

    private PlaceSearchViewWithOwnership toView(
            SearchCandidate candidate,
            Map<Long, PlaceOwnership> ownerships
    ) {
        Place place = candidate.place();
        PlaceSearchResult kakao = candidate.kakao();
        fillMissingCategory(place, kakao);
        PlaceOwnership ownership = place == null
                ? PlaceOwnership.none()
                : ownerships.getOrDefault(place.getId(), PlaceOwnership.none());
        String categoryGroupCode = firstNonBlank(
                place == null ? null : place.getCategoryGroupCode(),
                kakao == null ? null : kakao.categoryGroupCode()
        );
        String category = firstNonBlank(
                place == null ? null : place.getCategory(),
                kakao == null ? null : kakao.category()
        );
        PlaceSearchView view = new PlaceSearchView(
                place == null ? null : place.getId(),
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
                place == null ? List.of() : place.getImageUrls()
        );
        return new PlaceSearchViewWithOwnership(view, ownership);
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }

    private record SearchCandidate(
            Place place,
            PlaceSearchResult kakao
    ) {
    }

    private record PlaceSearchViewWithOwnership(
            PlaceSearchView view,
            PlaceOwnership ownership
    ) {
    }
}
