package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record TransitLeg(
        String type,
        String guidance,
        int distanceMeters,
        int durationSeconds,
        String vehicleName,
        String vehicleType,
        List<String> stops,
        List<RoutePoint> path
) {

    public TransitLeg {
        stops = stops == null ? List.of() : List.copyOf(stops);
        path = path == null ? List.of() : List.copyOf(path);
    }
}
