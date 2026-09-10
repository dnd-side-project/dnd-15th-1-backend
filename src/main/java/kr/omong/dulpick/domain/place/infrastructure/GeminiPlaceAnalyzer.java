package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ContentThumbnailDownloader;
import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceAnalyzer;
import kr.omong.dulpick.domain.place.application.exception.PlaceAnalysisUnavailableException;
import kr.omong.dulpick.domain.place.config.GeminiProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiPlaceAnalyzer implements PlaceAnalyzer {

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ContentThumbnailDownloader imageDownloader;

    @Autowired
    public GeminiPlaceAnalyzer(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("instagramThumbnailDownloader") ContentThumbnailDownloader imageDownloader
    ) {
        this(properties, objectMapper, imageDownloader, createRestClientBuilder(properties));
    }

    public GeminiPlaceAnalyzer(
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, null, createRestClientBuilder(properties));
    }

    GeminiPlaceAnalyzer(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            ContentThumbnailDownloader imageDownloader,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.imageDownloader = imageDownloader;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    private static RestClient.Builder createRestClientBuilder(GeminiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public String modelKey() {
        return properties.model();
    }

    @Override
    public String promptVersion() {
        return "place-extraction-v4-vision";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExtractedPlace> analyze(ContentMetadata metadata) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new PlaceAnalysisUnavailableException(null);
        }
        try {
            List<ExtractedPlace> textCandidates = requestAndParse(metadata, false);
            if (!textCandidates.isEmpty() || !shouldUseImageFallback(metadata)) {
                return textCandidates;
            }
            return requestAndParse(metadata, true);
        } catch (RuntimeException exception) {
            if (exception instanceof PlaceAnalysisUnavailableException failure) {
                throw failure;
            }
            throw new PlaceAnalysisUnavailableException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ExtractedPlace> requestAndParse(ContentMetadata metadata, boolean includeImages) {
        Map<String, Object> response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .build(properties.model()))
                    .header("x-goog-api-key", properties.apiKey())
                    .body(request(metadata, includeImages))
                    .retrieve()
                    .body(Map.class);
        return parseCandidates(responseText(response));
    }

    private boolean shouldUseImageFallback(ContentMetadata metadata) {
        return metadata.sourceType().name().startsWith("INSTAGRAM")
                && imageDownloader != null
                && !metadata.imageUrls().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<ExtractedPlace> parseCandidates(String json) {
        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
        Object candidates = parsed.get("candidates");
        if (!(candidates instanceof List<?> values)) {
            return List.of();
        }
        Map<String, ExtractedPlace> result = new LinkedHashMap<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> candidate)) {
                continue;
            }
            Object name = candidate.get("name");
            Object addressHint = candidate.get("addressHint");
            Object evidence = candidate.get("evidence");
            Object mentionType = candidate.get("mentionType");
            if (name != null && !name.toString().isBlank()) {
                ExtractedPlace extracted = new ExtractedPlace(
                        name.toString().strip(),
                        addressHint == null ? null : addressHint.toString().strip(),
                        evidence == null ? null : evidence.toString().strip(),
                        mentionType == null ? null : mentionType.toString().strip()
                );
                result.putIfAbsent(normalize(extracted.name()), extracted);
            }
        }
        return new ArrayList<>(result.values());
    }

    private Map<String, Object> request(ContentMetadata metadata, boolean includeImages) {
        boolean instagram = metadata.sourceType().name().startsWith("INSTAGRAM");
        String instructions = instagram && includeImages ? """
                Analyze an Instagram post or reel title, caption, and supplied images.
                Read visible text in supplied images (OCR) and use it as evidence when identifying places.
                Extract up to 20 distinct real-world venues or attractions explicitly supported by the text or images.
                Return each separately named place in a list. For a popup or event, return its host venue.
                A neighborhood, city, landmark mentioned only as context or comparison is not a venue.
                Do not infer a place from an influencer, product, hashtag, or generic scenery.
                Return a candidate only when the caption or image text names the venue itself or marks it as a location.
                """ : instagram ? """
                Analyze an Instagram post or reel title and caption.
                Extract up to 20 distinct real-world venues or attractions explicitly supported by the text.
                Return each separately named place in a list. For a popup or event, return its host venue.
                A neighborhood, city, landmark mentioned only as context or comparison is not a venue.
                Do not infer a place from an influencer, product, hashtag, or generic scenery.
                Return a candidate only when the caption names the venue itself or marks it as a location.
                """ : """
                Analyze the title and body text from a Naver map, Naver blog, or Tistory page.
                Extract up to 20 distinct real-world place names explicitly mentioned in the text.
                A Naver short map link identifies one place by its page title, so return only that title.
                For blogs, return each separately named venue or attraction and ignore article or product names.
                Do not infer a place that is not supported by the supplied text.
                """;
        String prompt = (instructions + """
                Return JSON only in this shape: {\"candidates\":[{\"name\":\"...\",\"addressHint\":\"...\",\"evidence\":\"...\",\"mentionType\":\"EXPLICIT_VENUE\"}]}.
                Content type: %s
                Title: %s
                Body or caption: %s
                """).formatted(
                metadata.sourceType(),
                safe(metadata.title()),
                safe(metadata.caption())
        );
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        if (includeImages) {
            parts.addAll(imageParts(metadata));
        }
        Map<String, Object> content = Map.of("role", "user", "parts", parts);
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema());
        if (!properties.model().startsWith("gemini-3.5-flash-lite")
                && !properties.model().startsWith("gemini-3.6-flash")) {
            generationConfig.put("temperature", 0.1);
        }
        Map<String, Object> request = new HashMap<>();
        request.put("contents", List.of(content));
        request.put("generationConfig", generationConfig);
        return request;
    }

    private List<Map<String, Object>> imageParts(ContentMetadata metadata) {
        if (!metadata.sourceType().name().startsWith("INSTAGRAM") || imageDownloader == null) {
            return List.of();
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        for (String imageUrl : metadata.imageUrls().stream().limit(10).toList()) {
            try {
                ContentThumbnailDownloader.DownloadedThumbnail image = imageDownloader.download(imageUrl);
                if (image.bytes().length > 1_500_000) {
                    continue;
                }
                parts.add(Map.of("inlineData", Map.of(
                        "mimeType", image.contentType().toString(),
                        "data", Base64.getEncoder().encodeToString(image.bytes())
                )));
            } catch (RuntimeException ignored) {
                // OCR is supplemental; place extraction must continue from text when an image expires.
            }
        }
        return parts;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> candidate = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "name", Map.of("type", "STRING"),
                        "addressHint", Map.of("type", "STRING"),
                        "evidence", Map.of("type", "STRING"),
                        "mentionType", Map.of("type", "STRING")
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

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }
}
