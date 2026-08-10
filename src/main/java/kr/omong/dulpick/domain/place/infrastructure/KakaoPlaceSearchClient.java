package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.PlaceSearcher;
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
import java.util.Map;

@Component
public class KakaoPlaceSearchClient implements PlaceSearcher {

    private final KakaoProperties properties;
    private final RestClient restClient;

    @Autowired
    public KakaoPlaceSearchClient(KakaoProperties properties) {
        this(properties, createRestClientBuilder(properties));
    }

    KakaoPlaceSearchClient(KakaoProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    private static RestClient.Builder createRestClientBuilder(KakaoProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
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
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
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

    private String text(Map<String, Object> document, String key) {
        Object value = document.get(key);
        return value == null ? "" : value.toString();
    }

    private BigDecimal decimal(Map<String, Object> document, String key) {
        String value = text(document, key);
        return value.isBlank() ? null : new BigDecimal(value);
    }
}
