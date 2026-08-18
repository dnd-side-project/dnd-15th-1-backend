package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceSearchServiceTest {

    private final PlaceSearcher placeSearcher = mock(PlaceSearcher.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceQueryService placeQueryService = mock(PlaceQueryService.class);
    private final RegionTagQueryService regionTagQueryService = mock(RegionTagQueryService.class);
    private final PlaceSearchService service = new PlaceSearchService(
            placeSearcher,
            placeRepository,
            placeQueryService,
            regionTagQueryService
    );

    @Test
    void putsDatabasePlaceFirstAndDeduplicatesSameKakaoPlace() {
        Place databasePlace = place(10L, "kakao-1", "DB 장소명", "서울 성동구 성수동");
        PlaceSearchResult sameKakaoPlace = searchResult(
                "kakao-1",
                "Kakao 장소명",
                "서울 성동구 성수동",
                "CE7",
                "음식점 > 카페"
        );
        PlaceSearchResult kakaoOnly = searchResult(
                "kakao-2",
                "Kakao 전용",
                "서울 마포구",
                "FD6",
                "음식점"
        );
        when(placeRepository.searchByKeyword(eq("카페"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(databasePlace)));
        when(placeSearcher.search("카페", 1)).thenReturn(new PlaceKeywordSearch(
                List.of(sameKakaoPlace, kakaoOnly),
                true
        ));
        when(placeRepository.findAllByKakaoPlaceIdIn(List.of("kakao-1", "kakao-2")))
                .thenReturn(List.of(databasePlace));
        when(placeQueryService.getOwnerships(1L, List.of(10L)))
                .thenReturn(Map.of(10L, PlaceOwnership.of(true, true, true)));
        when(regionTagQueryService.getTagsByPlaceIds(List.of(10L))).thenReturn(Map.of());
        when(regionTagQueryService.getActiveSummaries()).thenReturn(List.of());

        PlaceSearchPage result = service.search(1L, "카페", null, 0);

        assertThat(result.places()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.places().getFirst()).satisfies(place -> {
            assertThat(place.placeId()).isEqualTo(10L);
            assertThat(place.kakaoPlaceId()).isEqualTo("kakao-1");
            assertThat(place.name()).isEqualTo("DB 장소명");
            assertThat(place.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.TOGETHER);
            assertThat(place.savedByMe()).isTrue();
        });
        assertThat(result.places().get(1)).satisfies(place -> {
            assertThat(place.placeId()).isNull();
            assertThat(place.kakaoPlaceId()).isEqualTo("kakao-2");
            assertThat(place.ownershipStatus()).isNull();
            assertThat(place.savedByMe()).isFalse();
        });
    }

    @Test
    void appliesRegionTagFilter() {
        Place databasePlace = place(10L, "kakao-1", "성수 카페", "서울 성동구 성수동");
        RegionTagSummaryView seongsu = new RegionTagSummaryView(1L, "성수", 1);
        when(placeRepository.searchByKeyword(eq("성수"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(databasePlace)));
        when(placeSearcher.search("성수", 1)).thenReturn(new PlaceKeywordSearch(List.of(), true));
        when(placeRepository.findAllByKakaoPlaceIdIn(List.of("kakao-1")))
                .thenReturn(List.of(databasePlace));
        when(placeQueryService.getOwnerships(1L, List.of(10L)))
                .thenReturn(Map.of(10L, PlaceOwnership.of(true, false, true)));
        when(regionTagQueryService.getSummary(1L)).thenReturn(seongsu);
        when(regionTagQueryService.getTagsByPlaceIds(List.of(10L)))
                .thenReturn(Map.of(10L, List.of(seongsu)));
        when(regionTagQueryService.getActiveSummaries()).thenReturn(List.of(seongsu));

        PlaceSearchPage result = service.search(1L, "성수", 1L, 0);

        assertThat(result.places()).singleElement().satisfies(place -> {
            assertThat(place.categoryCode()).isEqualTo(DulpickPlaceCategory.CAFE);
            assertThat(place.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.TOGETHER);
            assertThat(place.savedByMe()).isFalse();
            assertThat(place.regionTags()).containsExactly(seongsu);
        });
    }

    @Test
    void requestsNextKakaoPageWithoutRepeatingDatabaseResults() {
        PlaceSearchResult nextPage = searchResult(
                "kakao-3",
                "다음 페이지 장소",
                "서울 성동구",
                "FD6",
                "음식점"
        );
        when(placeSearcher.search("성수", 2)).thenReturn(new PlaceKeywordSearch(
                List.of(nextPage),
                false
        ));
        when(placeRepository.findAllByKakaoPlaceIdIn(List.of("kakao-3"))).thenReturn(List.of());
        when(placeQueryService.getOwnerships(1L, List.of())).thenReturn(Map.of());
        when(regionTagQueryService.getTagsByPlaceIds(List.of())).thenReturn(Map.of());
        when(regionTagQueryService.getActiveSummaries()).thenReturn(List.of());

        PlaceSearchPage result = service.search(1L, "성수", null, 1);

        verify(placeRepository, never()).searchByKeyword(eq("성수"), any(Pageable.class));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.places()).singleElement().satisfies(place ->
                assertThat(place.kakaoPlaceId()).isEqualTo("kakao-3")
        );
    }

    private Place place(Long id, String kakaoPlaceId, String name, String address) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getKakaoPlaceId()).thenReturn(kakaoPlaceId);
        when(place.getName()).thenReturn(name);
        when(place.getAddress()).thenReturn(address);
        when(place.getRoadAddress()).thenReturn(address);
        when(place.getLatitude()).thenReturn(new BigDecimal("37.5446"));
        when(place.getLongitude()).thenReturn(new BigDecimal("127.0557"));
        when(place.getCategoryGroupCode()).thenReturn("CE7");
        when(place.getCategory()).thenReturn("음식점 > 카페");
        when(place.getImageUrls()).thenReturn(List.of());
        return place;
    }

    private PlaceSearchResult searchResult(
            String kakaoPlaceId,
            String name,
            String address,
            String categoryGroupCode,
            String category
    ) {
        return new PlaceSearchResult(
                kakaoPlaceId,
                name,
                address,
                address,
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557"),
                categoryGroupCode,
                category,
                "02-1234-5678",
                "https://place.map.kakao.com/" + kakaoPlaceId,
                null
        );
    }
}
