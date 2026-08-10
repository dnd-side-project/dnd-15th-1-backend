package kr.omong.dulpick.domain.place.application;

import java.util.List;

public interface PlaceSearcher {

    List<PlaceSearchResult> search(String query);
}
