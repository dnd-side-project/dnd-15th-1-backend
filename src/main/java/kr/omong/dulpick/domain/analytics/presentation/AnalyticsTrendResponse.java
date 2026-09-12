package kr.omong.dulpick.domain.analytics.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.analytics.application.AnalyticsTrendView;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsTrendResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Daily> days
) {

    public static AnalyticsTrendResponse from(List<AnalyticsTrendView> views) {
        return new AnalyticsTrendResponse(views.stream().map(Daily::from).toList());
    }

    public record Daily(
            @Schema(example = "2026-09-01")
            LocalDate date,
            @Schema(example = "150")
            long downloads,
            @Schema(example = "20")
            long newMembers,
            @Schema(example = "12")
            long connectedCouples,
            @Schema(example = "80")
            long placeViews,
            @Schema(example = "90")
            long savedPlaces,
            @Schema(example = "18")
            long createdDateCourses
    ) {

        private static Daily from(AnalyticsTrendView view) {
            return new Daily(
                    view.date(),
                    view.downloads(),
                    view.newMembers(),
                    view.connectedCouples(),
                    view.placeViews(),
                    view.savedPlaces(),
                    view.createdDateCourses()
            );
        }
    }
}
