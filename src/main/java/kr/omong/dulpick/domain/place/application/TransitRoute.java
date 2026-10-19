package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record TransitRoute(
        int distanceMeters,
        int durationSeconds,
        List<RoutePoint> path
) {

    public TransitRoute {
        path = path == null ? List.of() : List.copyOf(path);
    }
}
