package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceVerifier;
import kr.omong.dulpick.domain.place.application.PlaceSearcher;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.VerifiedPlace;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.KakaoProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Component
public class KakaoPlaceVerifier implements PlaceVerifier, PlaceSearcher {

    private final KakaoProperties properties;
    private final RestClient restClient;

    public KakaoPlaceVerifier(KakaoProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public VerifiedPlace verify(ExtractedPlace extractedPlace) {
        if (!properties.enabled()
                || properties.restApiKey() == null
                || properties.restApiKey().isBlank()) {
            throw new PlaceVerificationUnavailableException();
        }
        try {
            List<PlaceSearchResult> results = searchByNameFallback(extractedPlace.name());
            if (results.isEmpty() && extractedPlace.addressHint() != null) {
                results = search(query(extractedPlace));
            }
            List<PlaceSearchResult> searchResults = results;
            return results.stream()
                    .filter(result -> matches(extractedPlace, result))
                    .findFirst()
                    .or(() -> searchResults.stream()
                            .filter(result -> exactNameMatches(extractedPlace, result))
                            .findFirst())
                    .map(this::toVerifiedPlace)
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

    private List<PlaceSearchResult> searchByNameFallback(String name) {
        String[] words = name.strip().split("\\s+");
        for (int wordCount = words.length; wordCount >= 2; wordCount--) {
            String query = String.join(" ", java.util.Arrays.copyOf(words, wordCount));
            List<PlaceSearchResult> results = search(query);
            if (!results.isEmpty()) {
                return results;
            }
        }
        return search(name);
    }

    private boolean matches(
            ExtractedPlace extractedPlace,
            PlaceSearchResult result
    ) {
        String extractedName = normalize(extractedPlace.name());
        String resultName = normalize(result.name());
        boolean nameMatches = resultName.equals(extractedName)
                || resultName.contains(extractedName)
                || extractedName.contains(resultName);
        if (!nameMatches) {
            return false;
        }
        String addressHint = normalize(extractedPlace.addressHint());
        if (addressHint.isBlank()) {
            return true;
        }
        String address = normalize(result.address() + result.roadAddress());
        return address.contains(addressHint) || addressHint.contains(address);
    }

    private boolean exactNameMatches(
            ExtractedPlace extractedPlace,
            PlaceSearchResult result
    ) {
        return normalize(extractedPlace.name()).equals(normalize(result.name()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private PlaceSearchResult toSearchResult(Map<String, Object> document) {
        return new PlaceSearchResult(
                text(document, "id"),
                text(document, "place_name"),
                text(document, "address_name"),
                text(document, "road_address_name"),
                decimal(document, "y"),
                decimal(document, "x"),
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
