package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceAnalyzer;
import kr.omong.dulpick.domain.place.application.exception.PlaceAnalysisUnavailableException;
import kr.omong.dulpick.domain.place.config.GeminiProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiPlaceAnalyzer implements PlaceAnalyzer {

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiPlaceAnalyzer(
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExtractedPlace> analyze(ContentMetadata metadata) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new PlaceAnalysisUnavailableException(null);
        }
        try {
            Map<String, Object> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .build(properties.model()))
                    .header("x-goog-api-key", properties.apiKey())
                    .body(request(metadata))
                    .retrieve()
                    .body(Map.class);
            String json = responseText(response);
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            Object candidates = parsed.get("candidates");
            if (!(candidates instanceof List<?> values)) {
                return List.of();
            }
            List<ExtractedPlace> result = new ArrayList<>();
            for (Object value : values) {
                if (!(value instanceof Map<?, ?> candidate)) {
                    continue;
                }
                Object name = candidate.get("name");
                Object addressHint = candidate.get("addressHint");
                if (name != null && !name.toString().isBlank()) {
                    result.add(new ExtractedPlace(
                            name.toString().strip(),
                            addressHint == null ? null : addressHint.toString().strip()
                    ));
                }
            }
            return result;
        } catch (RuntimeException exception) {
            if (exception instanceof PlaceAnalysisUnavailableException failure) {
                throw failure;
            }
            throw new PlaceAnalysisUnavailableException(exception);
        }
    }

    private Map<String, Object> request(ContentMetadata metadata) {
        String prompt = """
                Extract up to 10 distinct real-world place candidates from the following Instagram text.
                If the text lists multiple places, return every separately named place as a separate candidate.
                Never collapse a numbered list or a list separated by commas into one candidate.
                For a popup or temporary event, return the host venue as the place candidate when the host venue
                is stated (for example, return '용산 아이파크몰' instead of only the popup or product name).
                Do not infer a place when the text does not support it.
                Return JSON only in this shape: {\"candidates\":[{\"name\":\"...\",\"addressHint\":\"...\"}]}.
                Content type: %s
                Title: %s
                Caption or description: %s
                """.formatted(
                metadata.sourceType(),
                safe(metadata.title()),
                safe(metadata.caption())
        );
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.1,
                "responseSchema", responseSchema()
        );
        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));
        request.put("generationConfig", generationConfig);
        return request;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> candidate = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "name", Map.of("type", "STRING"),
                        "addressHint", Map.of("type", "STRING")
                ),
                "required", List.of("name")
        );
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "candidates", Map.of(
                                "type", "ARRAY",
                                "items", candidate
                        )
                ),
                "required", List.of("candidates")
        );
    }

    @SuppressWarnings("unchecked")
    private String responseText(Map<String, Object> response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "{\"candidates\":[]}";
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return String.valueOf(parts.get(0).get("text"));
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }
}
