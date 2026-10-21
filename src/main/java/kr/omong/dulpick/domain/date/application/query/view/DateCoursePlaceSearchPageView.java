package kr.omong.dulpick.domain.date.application.query.view;

import java.util.List;

public record DateCoursePlaceSearchPageView(
        List<DateCoursePlaceCandidateView> places,
        int page,
        int size,
        boolean hasNext
) {
}
