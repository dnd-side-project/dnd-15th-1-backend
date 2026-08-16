package kr.omong.dulpick.domain.date.application.query.view;

import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;

public record DateCourseCategoryOptionView(
        DulpickPlaceCategory code,
        String name
) {
}
