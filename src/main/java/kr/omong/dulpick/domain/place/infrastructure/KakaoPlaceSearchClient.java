package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.PlaceKeywordSearch;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.PlaceSearcher;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.KakaoProperties;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class KakaoPlaceSearchClient implements PlaceSearcher {

    static final int SEARCH_SIZE = 10;
    private static final int MAX_PAGE = 45;
    private static final int BASE_CONCURRENT_SEARCHES = 16;
    private static final int VERIFICATION_PERMIT_BUFFER = 4;
    private static final Duration SEARCH_PERMIT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration QUERY_CACHE_TTL = Duration.ofMinutes(10);
    private static final int QUERY_CACHE_MAX_ENTRIES = 5_000;

    private final KakaoProperties properties;
    private final RestClient restClient;
    private final Clock clock;
    private final Semaphore searchPermits;
    private final Map<String, CachedResults> queryCache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedResults> eldest) {
                    return size() > QUERY_CACHE_MAX_ENTRIES;
                }
            }
    );
    private final Map<String, CompletableFuture<CachedResults>> inFlightQueries =
            new ConcurrentHashMap<>();

    @Autowired
    public KakaoPlaceSearchClient(
            KakaoProperties properties,
            Clock clock,
            PlaceAnalysisProperties analysisProperties
    ) {
        this(
                properties,
                createRestClientBuilder(properties),
                clock,
                resolvePermitCount(analysisProperties)
        );
    }

    KakaoPlaceSearchClient(KakaoProperties properties, RestClient.Builder restClientBuilder) {
        this(properties, restClientBuilder, Clock.systemUTC());
    }

    KakaoPlaceSearchClient(
            KakaoProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock
    ) {
        this(properties, restClientBuilder, clock, BASE_CONCURRENT_SEARCHES);
    }

    KakaoPlaceSearchClient(
            KakaoProperties properties,
            RestClient.Builder restClientBuilder,
            Clock clock,
            int maxConcurrentSearches
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.clock = clock;
        this.searchPermits = new Semaphore(maxConcurrentSearches);
    }

    private static RestClient.Builder createRestClientBuilder(KakaoProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    private static int resolvePermitCount(PlaceAnalysisProperties analysisProperties) {
        if (analysisProperties == null) {
            return BASE_CONCURRENT_SEARCHES;
        }
        return Math.max(
                BASE_CONCURRENT_SEARCHES,
                analysisProperties.verificationConcurrency() + VERIFICATION_PERMIT_BUFFER
        );
    }

    int searchPermitCount() {
        return searchPermits.availablePermits();
    }

    @Override
    public List<PlaceSearchResult> search(String query) {
        String cacheKey = query == null ? "" : query.strip();
        CachedResults cached = queryCache.get(cacheKey);
        if (cached != null && cached.isFresh(clock.instant())) {
            return cached.results();
        }
        return loadOnce(cacheKey).results();
    }

    private CachedResults loadOnce(String cacheKey) {
        CompletableFuture<CachedResults> myLoad = null;
        CompletableFuture<CachedResults> ongoingLoad = inFlightQueries.get(cacheKey);
        if (ongoingLoad == null) {
            myLoad = new CompletableFuture<>();
            ongoingLoad = inFlightQueries.putIfAbsent(cacheKey, myLoad);
            if (ongoingLoad == null) {
                try {
                    CachedResults fresh = fetchAndCache(cacheKey);
                    myLoad.complete(fresh);
                    return fresh;
                } catch (RuntimeException exception) {
                    myLoad.completeExceptionally(exception);
                    throw exception;
                } finally {
                    inFlightQueries.remove(cacheKey);
                }
            }
        }
        return awaitOngoingLoad(ongoingLoad);
    }

    private CachedResults fetchAndCache(String cacheKey) {
        List<PlaceSearchResult> results = search(cacheKey, FIRST_PAGE).results();
        CachedResults fresh = new CachedResults(
                List.copyOf(results),
                clock.instant().plus(QUERY_CACHE_TTL)
        );
        queryCache.put(cacheKey, fresh);
        return fresh;
    }

    private CachedResults awaitOngoingLoad(CompletableFuture<CachedResults> ongoingLoad) {
        try {
            return ongoingLoad.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PlaceKeywordSearch search(String query, int page) {
        if (!properties.enabled()
                || properties.restApiKey() == null
                || properties.restApiKey().isBlank()) {
            throw new PlaceVerificationUnavailableException();
        }
        int kakaoPage = Math.clamp(page, FIRST_PAGE, MAX_PAGE);
        if (!tryAcquireSearchPermit()) {
            throw new PlaceVerificationUnavailableException();
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("size", SEARCH_SIZE)
                            .queryParam("page", kakaoPage)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                return new PlaceKeywordSearch(List.of(), true);
            }
            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
            List<PlaceSearchResult> results = documents == null
                    ? List.of()
                    : documents.stream().map(this::toSearchResult).toList();
            return new PlaceKeywordSearch(results, isLastPage(response, results.size()));
        } catch (RestClientException exception) {
            throw new PlaceVerificationUnavailableException(exception);
        } finally {
            searchPermits.release();
        }
    }

    private boolean tryAcquireSearchPermit() {
        try {
            return searchPermits.tryAcquire(SEARCH_PERMIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private record CachedResults(List<PlaceSearchResult> results, Instant expiresAt) {

        boolean isFresh(Instant now) {
            return expiresAt.isAfter(now);
        }
    }

    private boolean isLastPage(Map<String, Object> response, int resultCount) {
        Object metaValue = response.get("meta");
        if (metaValue instanceof Map<?, ?> meta) {
            Object isEnd = meta.get("is_end");
            if (isEnd instanceof Boolean lastPage) {
                return lastPage;
            }
        }
        return resultCount < SEARCH_SIZE;
    }

    private PlaceSearchResult toSearchResult(Map<String, Object> document) {
        return new PlaceSearchResult(
                text(document, "id"),
                text(document, "place_name"),
                text(document, "address_name"),
                text(document, "road_address_name"),
                decimal(document, "y"),
                decimal(document, "x"),
                optionalText(document, "category_group_code"),
                text(document, "category_name"),
                optionalText(document, "phone"),
                optionalText(document, "place_url"),
                null
        );
    }

    private String text(Map<String, Object> document, String key) {
        Object value = document.get(key);
        return value == null ? "" : value.toString();
    }

    private String optionalText(Map<String, Object> document, String key) {
        String value = text(document, key);
        return value.isBlank() ? null : value;
    }

    private BigDecimal decimal(Map<String, Object> document, String key) {
        String value = text(document, key);
        return value.isBlank() ? null : new BigDecimal(value);
    }
}
