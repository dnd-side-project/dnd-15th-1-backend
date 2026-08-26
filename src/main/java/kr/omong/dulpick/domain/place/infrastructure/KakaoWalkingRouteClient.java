package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.WalkingRoute;
import kr.omong.dulpick.domain.place.application.WalkingRouteClient;
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
import java.util.Map;
import java.util.Optional;

@Component
public class KakaoWalkingRouteClient implements WalkingRouteClient {

    private static final Logger logger = LoggerFactory.getLogger(KakaoWalkingRouteClient.class);

    private final KakaoRoutingProperties properties;
    private final RestClient restClient;

    @Autowired
    public KakaoWalkingRouteClient(KakaoRoutingProperties properties) {
        this(properties, createRestClientBuilder(properties));
    }

    KakaoWalkingRouteClient(KakaoRoutingProperties properties, RestClient.Builder restClientBuilder) {
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
    public Optional<WalkingRoute> find(
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
                            .path("/v2/routing/walk")
                            .queryParam("start_x", startLongitude.toPlainString())
                            .queryParam("start_y", startLatitude.toPlainString())
                            .queryParam("end_x", endLongitude.toPlainString())
                            .queryParam("end_y", endLatitude.toPlainString())
                            .queryParam("route_mode", "SHORTEST")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                    .retrieve()
                    .body(Map.class);
            return parse(response);
        } catch (RestClientException exception) {
            logger.warn("Kakao walking route lookup failed", exception);
            return Optional.empty();
        }
    }

    private Optional<WalkingRoute> parse(Map<String, Object> response) {
        if (response == null) {
            return Optional.empty();
        }
        String status = text(response.get("status"));
        if ("SAME_POINT".equals(status)) {
            return Optional.of(new WalkingRoute(0, 0));
        }
        if (!status.isBlank() && !"OK".equals(status)) {
            logger.warn("Kakao walking route returned status={}", status);
            return Optional.empty();
        }
        Object routeValue = response.get("route");
        if (!(routeValue instanceof Map<?, ?> route)) {
            return Optional.empty();
        }
        Object propertiesValue = route.get("properties");
        if (!(propertiesValue instanceof Map<?, ?> routeProperties)) {
            return Optional.empty();
        }
        Integer distance = integer(routeProperties.get("totalDistance"));
        Integer duration = integer(routeProperties.get("totalTime"));
        if (distance == null || duration == null) {
            return Optional.empty();
        }
        return Optional.of(new WalkingRoute(distance, duration));
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
