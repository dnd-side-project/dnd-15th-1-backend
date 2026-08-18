package kr.omong.dulpick.domain.place.application;

import java.util.List;

public interface PlaceSearcher {

    int FIRST_PAGE = 1;

    PlaceKeywordSearch search(String query, int page);

    default List<PlaceSearchResult> search(String query) {
        return search(query, FIRST_PAGE).results();
    }
}
