package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.application.exception.WalkingRouteUnavailableException;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.WalkingRouteCache;
import kr.omong.dulpick.domain.place.domain.WalkingRouteCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PlaceWalkingRouteService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceWalkingRouteService.class);

    private final WalkingRouteClient walkingRouteClient;
    private final WalkingRouteCacheRepository walkingRouteCacheRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    public PlaceWalkingRouteService(
            WalkingRouteClient walkingRouteClient,
            WalkingRouteCacheRepository walkingRouteCacheRepository,
            PlaceRepository placeRepository,
            Clock clock
    ) {
        this.walkingRouteClient = walkingRouteClient;
        this.walkingRouteCacheRepository = walkingRouteCacheRepository;
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    @Transactional
    public WalkingRoute walkBetween(Long fromPlaceId, Long toPlaceId) {
        Place from = placeRepository.findById(fromPlaceId)
                .orElseThrow(PlaceNotFoundException::new);
        Place to = placeRepository.findById(toPlaceId)
                .orElseThrow(PlaceNotFoundException::new);
        WalkingRoute route = consecutiveWalks(List.of(from, to)).getFirst();
        if (route == null) {
            throw new WalkingRouteUnavailableException();
        }
        return route;
    }

    @Transactional
    public List<WalkingRoute> consecutiveWalks(List<Place> orderedPlaces) {
        if (orderedPlaces == null || orderedPlaces.size() < 2) {
            return orderedPlaces == null ? List.of() : nCopies(orderedPlaces.size());
        }
        List<PlacePair> pairs = new ArrayList<>();
        for (int index = 0; index < orderedPlaces.size() - 1; index++) {
            pairs.add(new PlacePair(orderedPlaces.get(index), orderedPlaces.get(index + 1)));
        }
        Map<PlacePairKey, WalkingRouteCache> caches = loadCaches(pairs);
        List<WalkingRoute> walks = new ArrayList<>(orderedPlaces.size());
        for (PlacePair pair : pairs) {
            walks.add(resolve(pair, caches.get(pair.key())));
        }
        walks.add(null);
        return java.util.Collections.unmodifiableList(walks);
    }

    private Map<PlacePairKey, WalkingRouteCache> loadCaches(List<PlacePair> pairs) {
        List<Long> fromIds = pairs.stream().map(pair -> pair.from().getId()).filter(Objects::nonNull).toList();
        List<Long> toIds = pairs.stream().map(pair -> pair.to().getId()).filter(Objects::nonNull).toList();
        if (fromIds.isEmpty() || toIds.isEmpty()) {
            return Map.of();
        }
        Map<PlacePairKey, WalkingRouteCache> caches = new HashMap<>();
        walkingRouteCacheRepository.findAllByFromPlaceIdInAndToPlaceIdIn(fromIds, toIds)
                .forEach(cache -> caches.put(
                        new PlacePairKey(cache.getFromPlaceId(), cache.getToPlaceId()),
                        cache
                ));
        return caches;
    }

    private WalkingRoute resolve(PlacePair pair, WalkingRouteCache cached) {
        Place from = pair.from();
        Place to = pair.to();
        if (!hasCoordinates(from) || !hasCoordinates(to)) {
            return null;
        }
        if (samePoint(from, to)) {
            return new WalkingRoute(0, 0);
        }
        if (cached != null && cached.matchesCoordinates(
                from.getLatitude(),
                from.getLongitude(),
                to.getLatitude(),
                to.getLongitude()
        )) {
            return new WalkingRoute(cached.getDistanceMeters(), cached.getDurationSeconds());
        }
        Optional<WalkingRoute> lookedUp = walkingRouteClient.find(
                from.getLongitude(),
                from.getLatitude(),
                to.getLongitude(),
                to.getLatitude()
        );
        if (lookedUp.isEmpty()) {
            logger.warn(
                    "Walking route unavailable from place {} to place {}",
                    from.getId(),
                    to.getId()
            );
            return null;
        }
        WalkingRoute route = lookedUp.get();
        persist(from, to, route, cached);
        return route;
    }

    private void persist(Place from, Place to, WalkingRoute route, WalkingRouteCache cached) {
        if (from.getId() == null || to.getId() == null) {
            return;
        }
        if (cached == null) {
            walkingRouteCacheRepository.save(WalkingRouteCache.create(
                    from.getId(),
                    to.getId(),
                    from.getLatitude(),
                    from.getLongitude(),
                    to.getLatitude(),
                    to.getLongitude(),
                    route.distanceMeters(),
                    route.durationSeconds(),
                    clock.instant()
            ));
            return;
        }
        cached.refresh(
                from.getLatitude(),
                from.getLongitude(),
                to.getLatitude(),
                to.getLongitude(),
                route.distanceMeters(),
                route.durationSeconds(),
                clock.instant()
        );
    }

    private boolean hasCoordinates(Place place) {
        return place != null && place.getLatitude() != null && place.getLongitude() != null;
    }

    private boolean samePoint(Place from, Place to) {
        return from.getLatitude().compareTo(to.getLatitude()) == 0
                && from.getLongitude().compareTo(to.getLongitude()) == 0;
    }

    private List<WalkingRoute> nCopies(int size) {
        List<WalkingRoute> walks = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            walks.add(null);
        }
        return java.util.Collections.unmodifiableList(walks);
    }

    private record PlacePair(Place from, Place to) {

        PlacePairKey key() {
            return new PlacePairKey(from.getId(), to.getId());
        }
    }

    private record PlacePairKey(Long fromPlaceId, Long toPlaceId) {
    }
}
