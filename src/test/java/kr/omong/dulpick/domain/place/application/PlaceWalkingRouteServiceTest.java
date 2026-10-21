package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.application.exception.WalkingRouteUnavailableException;
import kr.omong.dulpick.domain.place.config.KakaoRoutingProperties;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.WalkingRouteCache;
import kr.omong.dulpick.domain.place.domain.WalkingRouteCacheRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceWalkingRouteServiceTest {

    private final WalkingRouteClient walkingRouteClient = mock(WalkingRouteClient.class);
    private final TransitRouteClient transitRouteClient = mock(TransitRouteClient.class);
    private final WalkingRouteCacheRepository walkingRouteCacheRepository =
            mock(WalkingRouteCacheRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceWalkingRouteService service = new PlaceWalkingRouteService(
            walkingRouteClient,
            transitRouteClient,
            walkingRouteCacheRepository,
            placeRepository,
            Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC),
            new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30)
    );

    @Test
    void usesCachedRouteWithoutCallingKakaoAgain() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        WalkingRouteCache cache = WalkingRouteCache.create(
                1L,
                2L,
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0560000"),
                new BigDecimal("37.5480000"),
                new BigDecimal("127.0410000"),
                400,
                300,
                Instant.parse("2026-08-19T00:00:00Z")
        );
        cache.updatePath("");
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of(cache));

        List<WalkingRoute> walks = service.consecutiveWalks(List.of(from, to));

        assertThat(walks).containsExactly(new WalkingRoute(400, 300), null);
        verify(walkingRouteClient, never()).find(any(), any(), any(), any());
    }

    @Test
    void looksUpMissingPairsAndCachesResult() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(
                new BigDecimal("127.0560000"),
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0410000"),
                new BigDecimal("37.5480000")
        )).thenReturn(Optional.of(new WalkingRoute(4025, 3914)));

        List<WalkingRoute> walks = service.consecutiveWalks(List.of(from, to));

        assertThat(walks.getFirst()).isEqualTo(new WalkingRoute(4025, 3914));
        assertThat(walks.get(1)).isNull();
        verify(walkingRouteCacheRepository).save(any(WalkingRouteCache.class));
    }

    @Test
    void returnsWalkPathAndStoresItInCache() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        List<RoutePoint> path = List.of(new RoutePoint(37.5445, 127.056), new RoutePoint(37.548, 127.041));
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any()))
                .thenReturn(Optional.of(new WalkingRoute(400, 300, path)));

        List<WalkingRoute> walks = service.consecutiveWalks(List.of(from, to));

        assertThat(walks.getFirst().path()).isEqualTo(path);
        assertThat(walks.getFirst().transit()).isNull();
        verify(transitRouteClient, never()).find(any(), any(), any(), any());
        org.mockito.ArgumentCaptor<WalkingRouteCache> captor =
                org.mockito.ArgumentCaptor.forClass(WalkingRouteCache.class);
        verify(walkingRouteCacheRepository).save(captor.capture());
        assertThat(RoutePathCodec.decode(captor.getValue().getPathPoints())).isEqualTo(path);
    }

    @Test
    void looksUpTransitWhenWalkTakesThirtyMinutesOrMore() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        TransitRoute transit = new TransitRoute("SUBWAY", 5200, 1500, 0, 1550, List.of());
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any()))
                .thenReturn(Optional.of(new WalkingRoute(4025, 1800)));
        when(transitRouteClient.find(any(), any(), any(), any())).thenReturn(Optional.of(transit));

        List<WalkingRoute> walks = service.consecutiveWalks(List.of(from, to));

        assertThat(walks.getFirst().transit()).isEqualTo(transit);
    }

    @Test
    void doesNotStoreTransitAndLooksItUpAgainEvenWhenWalkIsCached() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        WalkingRouteCache cache = WalkingRouteCache.create(
                1L, 2L,
                new BigDecimal("37.5445000"), new BigDecimal("127.0560000"),
                new BigDecimal("37.5480000"), new BigDecimal("127.0410000"),
                4025, 3914, Instant.parse("2026-08-19T00:00:00Z")
        );
        cache.updatePath("37.5445,127.056;37.548,127.041");
        TransitRoute transit = new TransitRoute("BUS", 5200, 1500, 0, null, List.of());
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of(cache));
        when(transitRouteClient.find(any(), any(), any(), any())).thenReturn(Optional.of(transit));

        WalkingRoute walk = service.consecutiveWalks(List.of(from, to)).getFirst();

        assertThat(walk.path()).hasSize(2);
        assertThat(walk.transit()).isEqualTo(transit);
        verify(walkingRouteClient, never()).find(any(), any(), any(), any());
        verify(transitRouteClient).find(any(), any(), any(), any());
        verify(walkingRouteCacheRepository, never()).save(any());
    }

    @Test
    void keepsWalkWhenTransitLookupFails() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any()))
                .thenReturn(Optional.of(new WalkingRoute(4025, 3914)));
        when(transitRouteClient.find(any(), any(), any(), any())).thenReturn(Optional.empty());

        WalkingRoute walk = service.consecutiveWalks(List.of(from, to)).getFirst();

        assertThat(walk.durationSeconds()).isEqualTo(3914);
        assertThat(walk.transit()).isNull();
    }

    @Test
    void keepsWalkWhenTransitClientThrowsUnexpectedly() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any()))
                .thenReturn(Optional.of(new WalkingRoute(4025, 3914)));
        when(transitRouteClient.find(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("unexpected response shape"));

        WalkingRoute walk = service.consecutiveWalks(List.of(from, to)).getFirst();

        assertThat(walk.durationSeconds()).isEqualTo(3914);
        assertThat(walk.transit()).isNull();
    }

    @Test
    void keepsCourseWalksWhenKakaoLookupFails() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any())).thenReturn(Optional.empty());

        List<WalkingRoute> walks = service.consecutiveWalks(List.of(from, to));

        assertThat(walks).containsExactly(null, null);
        verify(walkingRouteCacheRepository, never()).save(any());
    }

    @Test
    void returnsZeroWithoutKakaoWhenCoordinatesAreIdentical() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5445000", "127.0560000");
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());

        List<WalkingRoute> walks = service.consecutiveWalks(List.of(from, to));

        assertThat(walks).containsExactly(new WalkingRoute(0, 0), null);
        verify(walkingRouteClient, never()).find(any(), any(), any(), any());
    }

    @Test
    void walkBetweenReturnsDistanceForTwoPlaces() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        when(placeRepository.findById(1L)).thenReturn(Optional.of(from));
        when(placeRepository.findById(2L)).thenReturn(Optional.of(to));
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any()))
                .thenReturn(Optional.of(new WalkingRoute(4025, 3914)));

        WalkingRoute route = service.walkBetween(1L, 2L);

        assertThat(route).isEqualTo(new WalkingRoute(4025, 3914));
    }

    @Test
    void walkBetweenThrowsWhenPlaceIsMissing() {
        when(placeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.walkBetween(1L, 2L))
                .isInstanceOf(PlaceNotFoundException.class);
    }

    @Test
    void walkBetweenThrowsWhenKakaoLookupFails() {
        Place from = place(1L, "37.5445000", "127.0560000");
        Place to = place(2L, "37.5480000", "127.0410000");
        when(placeRepository.findById(1L)).thenReturn(Optional.of(from));
        when(placeRepository.findById(2L)).thenReturn(Optional.of(to));
        when(walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(List.of(1L), List.of(2L)))
                .thenReturn(List.of());
        when(walkingRouteClient.find(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.walkBetween(1L, 2L))
                .isInstanceOf(WalkingRouteUnavailableException.class);
    }

    private Place place(Long id, String latitude, String longitude) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getLatitude()).thenReturn(new BigDecimal(latitude));
        when(place.getLongitude()).thenReturn(new BigDecimal(longitude));
        return place;
    }
}
