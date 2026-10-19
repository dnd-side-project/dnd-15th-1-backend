package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.TransitLeg;
import kr.omong.dulpick.domain.place.application.TransitRoute;
import kr.omong.dulpick.domain.place.application.TransitRouteClient;
import kr.omong.dulpick.domain.place.config.KakaoRoutingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kakao 대중교통 경로 조회 API(/v2/routing/publictraffic) 클라이언트입니다.
 * Kakao 정책상 응답을 저장하지 않고 매번 실시간으로 호출합니다.
 */
@Component
public class KakaoTransitRouteClient implements TransitRouteClient {

    private static final Logger logger = LoggerFactory.getLogger(KakaoTransitRouteClient.class);

    private final KakaoRoutingProperties properties;
    private final RestClient restClient;

    @Autowired
    public KakaoTransitRouteClient(KakaoRoutingProperties properties) {
        this(properties, createRestClientBuilder(properties));
    }

    KakaoTransitRouteClient(KakaoRoutingProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    private static RestClient.Builder createRestClientBuilder(KakaoRoutingProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<TransitRoute> find(
            BigDecimal startLongitude,
            BigDecimal startLatitude,
            BigDecimal endLongitude,
            BigDecimal endLatitude
    ) {
        if (!properties.enabled()
                || properties.restApiKey() == null
                || properties.restApiKey().isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/routing/publictraffic")
                            .queryParam("start_x", startLongitude.toPlainString())
                            .queryParam("start_y", startLatitude.toPlainString())
                            .queryParam("end_x", endLongitude.toPlainString())
                            .queryParam("end_y", endLatitude.toPlainString())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                    .retrieve()
                    .body(Map.class);
            return parse(response);
        } catch (RestClientException exception) {
            logger.warn("Kakao transit route lookup failed", exception);
            return Optional.empty();
        }
    }

    private Optional<TransitRoute> parse(Map<String, Object> response) {
        if (response == null) {
            return Optional.empty();
        }
        String status = text(response.get("status"));
        if (!"OK".equals(status)) {
            logger.warn("Kakao transit route returned status={}", status);
            return Optional.empty();
        }
        if (!(response.get("routes") instanceof List<?> routes) || routes.isEmpty()) {
            return Optional.empty();
        }
        if (!(routes.getFirst() instanceof Map<?, ?> route)
                || !(route.get("properties") instanceof Map<?, ?> routeProperties)) {
            return Optional.empty();
        }
        Integer distance = integer(routeProperties.get("totalDistance"));
        Integer duration = integer(routeProperties.get("totalTime"));
        if (distance == null || duration == null) {
            return Optional.empty();
        }
        Integer transfers = integer(routeProperties.get("transfers"));
        Integer fare = routeProperties.get("fare") instanceof Map<?, ?> fareMap
                ? integer(fareMap.get("value"))
                : null;
        return Optional.of(new TransitRoute(
                text(routeProperties.get("type")),
                distance,
                duration,
                transfers == null ? 0 : transfers,
                fare,
                legs(route.get("steps"))
        ));
    }

    private List<TransitLeg> legs(Object stepsValue) {
        if (!(stepsValue instanceof List<?> steps)) {
            return List.of();
        }
        List<TransitLeg> legs = new ArrayList<>(steps.size());
        for (Object stepValue : steps) {
            if (!(stepValue instanceof Map<?, ?> step)
                    || !(step.get("properties") instanceof Map<?, ?> stepProperties)) {
                continue;
            }
            Integer distance = integer(stepProperties.get("distance"));
            Integer time = integer(stepProperties.get("time"));
            Map<?, ?> vehicle = firstMap(stepProperties.get("vehicles"));
            Object points = step.get("path") instanceof Map<?, ?> path ? path.get("points") : null;
            legs.add(new TransitLeg(
                    text(stepProperties.get("type")),
                    text(stepProperties.get("guidance")),
                    distance == null ? 0 : distance,
                    time == null ? 0 : time,
                    vehicle == null ? null : text(vehicle.get("name")),
                    vehicle == null ? null : text(vehicle.get("type")),
                    stopNames(stepProperties.get("stops")),
                    KakaoRoutePathParser.parse(points)
            ));
        }
        return List.copyOf(legs);
    }

    private Map<?, ?> firstMap(Object value) {
        if (value instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> map) {
            return map;
        }
        return null;
    }

    private List<String> stopNames(Object value) {
        if (!(value instanceof List<?> stops)) {
            return List.of();
        }
        List<String> names = new ArrayList<>(stops.size());
        for (Object stop : stops) {
            if (stop instanceof Map<?, ?> map && !text(map.get("name")).isBlank()) {
                names.add(text(map.get("name")));
            }
        }
        return names;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
