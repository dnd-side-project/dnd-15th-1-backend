package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.WalkingRoute;

public record WalkingRouteResponse(
        @Schema(description = "출발 공용 장소 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        Long fromPlaceId,
        @Schema(description = "도착 공용 장소 ID", example = "102", requiredMode = Schema.RequiredMode.REQUIRED)
        Long toPlaceId,
        @Schema(description = "도보 이동거리(미터)", example = "4025", requiredMode = Schema.RequiredMode.REQUIRED)
        int distanceMeters,
        @Schema(description = "도보 이동시간(초)", example = "3914", requiredMode = Schema.RequiredMode.REQUIRED)
        int durationSeconds
) {

    public static WalkingRouteResponse from(Long fromPlaceId, Long toPlaceId, WalkingRoute route) {
        return new WalkingRouteResponse(
                fromPlaceId,
                toPlaceId,
                route.distanceMeters(),
                route.durationSeconds()
        );
    }
}
