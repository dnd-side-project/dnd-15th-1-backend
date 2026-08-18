package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.PlaceVerifier;
import kr.omong.dulpick.domain.place.application.PlaceVerificationResult;
import kr.omong.dulpick.domain.place.application.VerifiedPlace;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.KakaoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class KakaoPlaceVerifier implements PlaceVerifier {

    private final KakaoProperties properties;
    private final KakaoPlaceSearchClient searchClient;
    private final KakaoPlaceMatcher placeMatcher;

    @Autowired
    public KakaoPlaceVerifier(
            KakaoProperties properties,
            KakaoPlaceSearchClient searchClient
    ) {
        this(properties, searchClient, new KakaoPlaceMatcher());
    }

    KakaoPlaceVerifier(
            KakaoProperties properties,
            KakaoPlaceSearchClient searchClient,
            KakaoPlaceMatcher placeMatcher
    ) {
        this.properties = properties;
        this.searchClient = searchClient;
        this.placeMatcher = placeMatcher;
    }

    @Override
    public PlaceVerificationResult verify(ExtractedPlace extractedPlace) {
        if (!properties.enabled()
                || properties.restApiKey() == null
                || properties.restApiKey().isBlank()) {
            throw new PlaceVerificationUnavailableException();
        }
        try {
            List<PlaceSearchResult> results = searchCandidates(extractedPlace);
            return placeMatcher.findBest(extractedPlace, results)
                    .map(match -> new PlaceVerificationResult(
                            toVerifiedPlace(match.place()),
                            match.status()
                    ))
                    .orElse(null);
        } catch (PlaceVerificationUnavailableException exception) {
            throw exception;
        }
    }

    private String query(ExtractedPlace extractedPlace) {
        if (extractedPlace.addressHint() == null || extractedPlace.addressHint().isBlank()) {
            return extractedPlace.name();
        }
        return extractedPlace.name() + " " + extractedPlace.addressHint();
    }

    private List<PlaceSearchResult> searchCandidates(ExtractedPlace extractedPlace) {
        Map<String, PlaceSearchResult> results = new LinkedHashMap<>();
        Set<String> searchedQueries = new LinkedHashSet<>();
        searchAndAdd(results, searchedQueries, query(extractedPlace));
        if (extractedPlace.addressHint() != null && !extractedPlace.addressHint().isBlank()) {
            searchAndAdd(
                    results,
                    searchedQueries,
                    extractedPlace.name() + " " + region(extractedPlace.addressHint())
            );
        }
        List<PlaceSearchResult> nameResults = searchAndAdd(
                results,
                searchedQueries,
                extractedPlace.name()
        );
        if (placeMatcher.hasPreciseAddress(extractedPlace.addressHint())) {
            searchAndAdd(results, searchedQueries, extractedPlace.addressHint());
        }
        if (nameResults.isEmpty()) {
            searchShorterNames(results, searchedQueries, extractedPlace.name());
        }
        return results.values().stream().toList();
    }

    private List<PlaceSearchResult> searchAndAdd(
            Map<String, PlaceSearchResult> target,
            Set<String> searchedQueries,
            String query
    ) {
        String normalizedQuery = query == null ? "" : query.strip();
        if (normalizedQuery.isBlank() || !searchedQueries.add(normalizedQuery)) {
            return List.of();
        }
        List<PlaceSearchResult> results = searchClient.search(normalizedQuery);
        addResults(target, results);
        return results;
    }

    private void addResults(Map<String, PlaceSearchResult> target, List<PlaceSearchResult> results) {
        results.forEach(result -> target.putIfAbsent(result.kakaoPlaceId(), result));
    }

    private String region(String addressHint) {
        String[] words = addressHint.strip().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOf(words, Math.min(words.length, 2)));
    }

    private void searchShorterNames(
            Map<String, PlaceSearchResult> target,
            Set<String> searchedQueries,
            String name
    ) {
        String[] words = name.strip().split("\\s+");
        for (int wordCount = words.length - 1; wordCount >= 2; wordCount--) {
            String query = String.join(" ", java.util.Arrays.copyOf(words, wordCount));
            List<PlaceSearchResult> results = searchAndAdd(target, searchedQueries, query);
            if (!results.isEmpty()) {
                return;
            }
        }
    }

    private VerifiedPlace toVerifiedPlace(PlaceSearchResult result) {
        return new VerifiedPlace(
                result.kakaoPlaceId(),
                result.name(),
                result.address(),
                result.roadAddress(),
                result.latitude(),
                result.longitude(),
                result.categoryGroupCode(),
                result.category(),
                result.phone(),
                result.kakaoPlaceUrl(),
                result.thumbnailUrl()
        );
    }

}
