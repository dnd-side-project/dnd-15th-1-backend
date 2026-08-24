package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceDetailQueryServiceTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceQueryService placeQueryService = mock(PlaceQueryService.class);
    private final PlaceSearchService placeSearchService = mock(PlaceSearchService.class);
    private final PlaceDetailQueryService service = new PlaceDetailQueryService(
            placeRepository,
            placeQueryService,
            placeSearchService
    );

    @Test
    void returnsDatabaseDetailsWithCurrentCoupleOwnership() {
        Place place = place();
        when(placeRepository.findById(10L)).thenReturn(Optional.of(place));
        when(placeQueryService.getOwnerships(1L, List.of(10L)))
                .thenReturn(Map.of(10L, PlaceOwnership.of(true, false, true)));
        when(placeQueryService.savedMemberCount(10L)).thenReturn(2);

        PlaceDetailView result = service.get(1L, 10L);

        assertThat(result.placeId()).isEqualTo(10L);
        assertThat(result.kakaoPlaceId()).isEqualTo("kakao-10");
        assertThat(result.phone()).isEqualTo("02-1234-5678");
        assertThat(result.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.TOGETHER);
        assertThat(result.savedByMe()).isFalse();
        assertThat(result.savedMemberCount()).isEqualTo(2);
    }

    @Test
    void returnsKakaoOnlyDetailsWithoutDatabaseWrite() {
        PlaceSearchResult kakao = new PlaceSearchResult(
                "kakao-20",
                "미저장 장소",
                "서울 성동구 성수동",
                "서울 성동구 성수길",
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557"),
                "CE7",
                "음식점 > 카페",
                "02-1111-2222",
                "https://place.map.kakao.com/kakao-20",
                null
        );
        when(placeSearchService.resolve("성수 카페", "kakao-20")).thenReturn(kakao);
        when(placeRepository.findByKakaoPlaceId("kakao-20")).thenReturn(Optional.empty());

        PlaceDetailView result = service.getByKakaoPlaceId(
                1L,
                "kakao-20",
                "성수 카페"
        );

        assertThat(result.placeId()).isNull();
        assertThat(result.savedByMe()).isFalse();
        assertThat(result.ownershipStatus()).isNull();
    }

    @Test
    void prefersFreshKakaoCategoryWhenStoredPlaceCategoryGroupCodeIsMissing() {
        Place place = place();
        when(place.getCategoryGroupCode()).thenReturn(null);
        PlaceSearchResult kakao = new PlaceSearchResult(
                "kakao-10",
                "저장 장소",
                "서울 성동구 성수동",
                "서울 성동구 성수길",
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557"),
                "CE7",
                "음식점 > 카페",
                "02-1234-5678",
                "https://place.map.kakao.com/kakao-10",
                null
        );
        when(placeSearchService.resolve("성수 카페", "kakao-10")).thenReturn(kakao);
        when(placeRepository.findByKakaoPlaceId("kakao-10")).thenReturn(Optional.of(place));
        when(placeQueryService.getOwnerships(1L, List.of(10L))).thenReturn(Map.of());
        when(placeQueryService.savedMemberCount(10L)).thenReturn(0);

        PlaceDetailView result = service.getByKakaoPlaceId(1L, "kakao-10", "성수 카페");

        assertThat(result.categoryCode()).isEqualTo(
                kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory.CAFE
        );
    }

    @Test
    void writesRecognizedFreshCategoryWhenStoredCategoryIsMissing() {
        PlaceCategoryWriteThroughService categoryWriter = mock(PlaceCategoryWriteThroughService.class);
        PlaceDetailQueryService serviceWithWriter = new PlaceDetailQueryService(
                placeRepository,
                placeQueryService,
                placeSearchService,
                categoryWriter
        );
        Place place = place();
        when(place.getCategoryGroupCode()).thenReturn(null);
        when(place.getCategory()).thenReturn(null);
        PlaceSearchResult kakao = new PlaceSearchResult(
                "kakao-10",
                "저장 장소",
                "서울 성동구 성수동",
                "서울 성동구 성수길",
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557"),
                "CE7",
                "음식점 > 카페",
                "02-1234-5678",
                "https://place.map.kakao.com/kakao-10",
                null
        );
        when(placeSearchService.resolve("성수 카페", "kakao-10")).thenReturn(kakao);
        when(placeRepository.findByKakaoPlaceId("kakao-10")).thenReturn(Optional.of(place));
        when(placeQueryService.getOwnerships(1L, List.of(10L))).thenReturn(Map.of());
        when(placeQueryService.savedMemberCount(10L)).thenReturn(0);

        serviceWithWriter.getByKakaoPlaceId(1L, "kakao-10", "성수 카페");

        org.mockito.Mockito.verify(categoryWriter).fillIfMissing(
                10L,
                null,
                null,
                "CE7",
                "음식점 > 카페"
        );
    }

    @Test
    void returnsDatabasePlacesMatchingCoordinates() {
        Place place = place();
        when(placeRepository.findAllByLatitudeAndLongitude(
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000")
        )).thenReturn(List.of(place));
        when(placeQueryService.getOwnerships(1L, List.of(10L)))
                .thenReturn(Map.of(10L, PlaceOwnership.of(false, true, false)));
        when(placeQueryService.savedMemberCount(10L)).thenReturn(0);

        List<PlaceDetailView> result = service.findByCoordinates(
                1L,
                new BigDecimal("37.5446"),
                new BigDecimal("127.0557")
        );

        assertThat(result).singleElement().satisfies(found -> {
            assertThat(found.placeId()).isEqualTo(10L);
            assertThat(found.savedByMe()).isTrue();
        });
    }

    @Test
    void returnsEmptyListWhenNoDatabasePlaceMatchesCoordinates() {
        when(placeRepository.findAllByLatitudeAndLongitude(
                new BigDecimal("37.0000000"),
                new BigDecimal("127.0000000")
        )).thenReturn(List.of());

        assertThat(service.findByCoordinates(
                1L,
                new BigDecimal("37.0000"),
                new BigDecimal("127.0000")
        )).isEmpty();
    }

    private Place place() {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(10L);
        when(place.getKakaoPlaceId()).thenReturn("kakao-10");
        when(place.getName()).thenReturn("저장 장소");
        when(place.getAddress()).thenReturn("서울 성동구 성수동");
        when(place.getRoadAddress()).thenReturn("서울 성동구 성수길");
        when(place.getLatitude()).thenReturn(new BigDecimal("37.5446"));
        when(place.getLongitude()).thenReturn(new BigDecimal("127.0557"));
        when(place.getCategoryGroupCode()).thenReturn("CE7");
        when(place.getCategory()).thenReturn("음식점 > 카페");
        when(place.getPhone()).thenReturn("02-1234-5678");
        when(place.getKakaoPlaceUrl()).thenReturn("https://place.map.kakao.com/kakao-10");
        when(place.getImageUrls()).thenReturn(List.of());
        return place;
    }
}
