package kr.omong.dulpick.domain.place.application;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaceSearchService {

    private final PlaceSearcher placeSearcher;

    public PlaceSearchService(PlaceSearcher placeSearcher) {
        this.placeSearcher = placeSearcher;
    }

    public List<PlaceSearchResult> search(String query) {
        return placeSearcher.search(query);
    }

    public PlaceSearchResult resolve(String query, String kakaoPlaceId) {
        return search(query).stream()
                .filter(result -> result.kakaoPlaceId().equals(kakaoPlaceId))
                .findFirst()
                .orElseThrow(kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException::new);
    }
}
