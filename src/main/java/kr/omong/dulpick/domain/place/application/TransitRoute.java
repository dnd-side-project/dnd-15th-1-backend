package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record TransitRoute(
        String type,
        int distanceMeters,
        int durationSeconds,
        int transfers,
        Integer fare,
        List<TransitLeg> legs
) {

    public TransitRoute {
        legs = legs == null ? List.of() : List.copyOf(legs);
    }
}
