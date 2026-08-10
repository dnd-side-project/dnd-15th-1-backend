package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceVerifier;
import kr.omong.dulpick.domain.place.application.PlaceSearcher;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.PlaceVerificationResult;
import kr.omong.dulpick.domain.place.application.VerifiedPlace;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.KakaoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class KakaoPlaceVerifier implements PlaceVerifier, PlaceSearcher {

    private final KakaoProperties properties;
    private final RestClient restClient;
    private final KakaoPlaceMatcher placeMatcher;

    @Autowired
    public KakaoPlaceVerifier(KakaoProperties properties) {
        this(properties, createRestClientBuilder(properties), new KakaoPlaceMatcher());
    }

    KakaoPlaceVerifier(
            KakaoProperties properties,
            RestClient.Builder restClientBuilder,
            KakaoPlaceMatcher placeMatcher
    ) {
        this.properties = properties;
        this.placeMatcher = placeMatcher;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    private static RestClient.Builder createRestClientBuilder(KakaoProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
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
        } catch (RestClientException exception) {
            throw new PlaceVerificationUnavailableException(exception);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PlaceSearchResult> search(String query) {
        if (!properties.enabled()
                || properties.restApiKey() == null
                || properties.restApiKey().isBlank()) {
            throw new PlaceVerificationUnavailableException();
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("size", 5)
                            .build())
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "KakaoAK " + properties.restApiKey()
                    )
                    .retrieve()
                    .body(Map.class);
            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
            if (documents == null) {
                return List.of();
            }
            return documents.stream().map(this::toSearchResult).toList();
        } catch (RestClientException exception) {
            throw new PlaceVerificationUnavailableException(exception);
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
        if (nameResults.isEmpty()) {
            searchShorterNames(results, searchedQueries, extractedPlace.name());
        }
        if (placeMatcher.hasPreciseAddress(extractedPlace.addressHint())) {
            searchAndAdd(results, searchedQueries, extractedPlace.addressHint());
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
        List<PlaceSearchResult> results = search(normalizedQuery);
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

    private PlaceSearchResult toSearchResult(Map<String, Object> document) {
        return new PlaceSearchResult(
                text(document, "id"),
                text(document, "place_name"),
                text(document, "address_name"),
                text(document, "road_address_name"),
                decimal(document, "y"),
                decimal(document, "x"),
                text(document, "category_group_code"),
                text(document, "category_name"),
                null
        );
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
                result.thumbnailUrl()
        );
    }

    private String text(Map<String, Object> document, String key) {
        Object value = document.get(key);
        return value == null ? "" : value.toString();
    }

    private BigDecimal decimal(Map<String, Object> document, String key) {
        String value = text(document, key);
        return value.isBlank() ? null : new BigDecimal(value);
    }
}
