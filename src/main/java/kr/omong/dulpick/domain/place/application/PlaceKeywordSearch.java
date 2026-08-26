package kr.omong.dulpick.domain.place.application;

import java.util.List;

public record PlaceKeywordSearch(
        List<PlaceSearchResult> results,
        boolean lastPage
) {
}
