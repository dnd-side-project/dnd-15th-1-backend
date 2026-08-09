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
import java.util.Comparator;
import java.util.LinkedHashMap;
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
            List<PlaceSearchResult> results = searchCandidates(extractedPlace);
            return results.stream()
                    .map(result -> new RankedPlace(result, score(extractedPlace, result)))
                    .sorted(Comparator.comparingInt(RankedPlace::score).reversed())
                    .map(RankedPlace::place)
                    .findFirst()
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

    private List<PlaceSearchResult> searchCandidates(ExtractedPlace extractedPlace) {
        Map<String, PlaceSearchResult> results = new LinkedHashMap<>();
        addResults(results, search(query(extractedPlace)));
        if (extractedPlace.addressHint() != null && !extractedPlace.addressHint().isBlank()) {
            addResults(results, search(extractedPlace.name() + " " + region(extractedPlace.addressHint())));
        }
        addResults(results, searchByNameFallback(extractedPlace.name()));
        return results.values().stream().toList();
    }

    private void addResults(Map<String, PlaceSearchResult> target, List<PlaceSearchResult> results) {
        results.forEach(result -> target.putIfAbsent(result.kakaoPlaceId(), result));
    }

    private String region(String addressHint) {
        String[] words = addressHint.strip().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOf(words, Math.min(words.length, 2)));
    }

    private int score(ExtractedPlace extractedPlace, PlaceSearchResult result) {
        int score = 0;
        String extractedName = normalize(extractedPlace.name());
        String resultName = normalize(result.name());
        if (resultName.equals(extractedName)) {
            score += 25;
        } else if (resultName.contains(extractedName) || extractedName.contains(resultName)) {
            score += 15;
        }
        String rawEvidence = extractedPlace.evidence() == null ? "" : extractedPlace.evidence().strip();
        String evidence = normalize(rawEvidence);
        if (evidence.isBlank()) {
            score -= 40;
        } else {
            if (evidence.contains(extractedName)) {
                score += 30;
            }
            if (rawEvidence.contains("위치") || rawEvidence.contains("주소") || rawEvidence.contains("📍")) {
                score += 20;
            }
        }
        String addressHint = normalize(extractedPlace.addressHint());
        if (!addressHint.isBlank()) {
            String address = normalize(result.address() + result.roadAddress());
            score += address.contains(addressHint) || addressHint.contains(address) ? 20 : -30;
        }
        if ("EXPLICIT_VENUE".equalsIgnoreCase(extractedPlace.mentionType())) {
            score += 10;
        }
        return score;
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

    private record RankedPlace(PlaceSearchResult place, int score) {
    }
}
