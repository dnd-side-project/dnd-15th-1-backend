package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceDateTraitsView;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;

public record PlaceDateTraitsResponse(
        @Schema(
                description = "네 축 분류 상태",
                allowableValues = {"UNCLASSIFIED", "PARTIALLY_CLASSIFIED", "CLASSIFIED"},
                example = "CLASSIFIED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PlaceClassificationStatus status,
        @Schema(
                description = "실내·실외. 미분류면 null",
                allowableValues = {"INDOOR", "OUTDOOR"},
                example = "INDOOR",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        PlaceEnvironment environment,
        @Schema(
                description = "액티비티·정적. 미분류면 null",
                allowableValues = {"ACTIVE", "STATIC"},
                example = "STATIC",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        PlaceActivity activity,
        @Schema(
                description = "낮·밤. 미분류면 null",
                allowableValues = {"DAY", "NIGHT"},
                example = "DAY",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        PlaceTime time,
        @Schema(
                description = "식사·볼거리. 미분류면 null",
                allowableValues = {"FOOD", "SIGHTSEEING"},
                example = "FOOD",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        PlaceFocus focus
) {

    public static PlaceDateTraitsResponse from(PlaceDateTraitsView view) {
        PlaceDateTraitsView traits = view == null ? PlaceDateTraitsView.unclassified() : view;
        return new PlaceDateTraitsResponse(
                traits.status(),
                traits.environment(),
                traits.activity(),
                traits.time(),
                traits.focus()
        );
    }
}
