package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record PlaceSearchPage(
        List<PlaceSearchView> places,
        int page,
        int size,
        boolean hasNext
) {
}
