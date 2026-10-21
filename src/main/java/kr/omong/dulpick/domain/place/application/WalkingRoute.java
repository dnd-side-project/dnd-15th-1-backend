package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record WalkingRoute(
        int distanceMeters,
        int durationSeconds,
        List<RoutePoint> path,
        TransitRoute transit
) {

    public WalkingRoute {
        path = path == null ? List.of() : List.copyOf(path);
    }

    public WalkingRoute(int distanceMeters, int durationSeconds) {
        this(distanceMeters, durationSeconds, List.of(), null);
    }

    public WalkingRoute(int distanceMeters, int durationSeconds, List<RoutePoint> path) {
        this(distanceMeters, durationSeconds, path, null);
    }

    public WalkingRoute withTransit(TransitRoute transit) {
        return new WalkingRoute(distanceMeters, durationSeconds, path, transit);
    }
}
