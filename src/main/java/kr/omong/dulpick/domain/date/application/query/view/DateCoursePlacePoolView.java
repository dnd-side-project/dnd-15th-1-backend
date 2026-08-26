package kr.omong.dulpick.domain.date.application.query.view;

import java.util.List;

public record DateCoursePlacePoolView(
        List<DateCoursePlaceCandidateView> places,
        List<String> availableRegions,
        List<DateCourseCategoryOptionView> availableCategories
) {

    public DateCoursePlacePoolView {
        places = places == null ? List.of() : List.copyOf(places);
        availableRegions = availableRegions == null ? List.of() : List.copyOf(availableRegions);
        availableCategories = availableCategories == null
                ? List.of()
                : List.copyOf(availableCategories);
    }
}
