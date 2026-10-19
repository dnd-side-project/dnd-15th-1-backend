package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.RoutePoint;
import kr.omong.dulpick.domain.place.application.TransitRoute;
import kr.omong.dulpick.domain.place.config.KakaoRoutingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoTransitRouteClientTest {

    @Test
    void returnsDistanceAndDurationFromKakaoTransitRoute() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/transit")))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("start_x", "127.0560000"))
                .andExpect(queryParam("start_y", "37.5445000"))
                .andExpect(queryParam("end_x", "127.0410000"))
                .andExpect(queryParam("end_y", "37.5480000"))
                .andRespond(withSuccess("""
                        {
                          "status": "OK",
                          "route": {
                            "properties": {
                              "totalDistance": 4025,
                              "totalTime": 3914
                            },
                            "path": [
                              {"x": 127.056, "y": 37.5445},
                              {"x": 127.041, "y": 37.548}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<TransitRoute> route = client.find(
                new BigDecimal("127.0560000"),
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0410000"),
                new BigDecimal("37.5480000")
        );

        assertThat(route).contains(new TransitRoute(4025, 3914, List.of(
                new RoutePoint(37.5445, 127.056),
                new RoutePoint(37.548, 127.041)
        )));
        server.verify();
    }

    @Test
    void returnsEmptyWhenKakaoTransitRouteFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/transit")))
                .andRespond(withServerError());

        Optional<TransitRoute> route = client.find(
                new BigDecimal("127.1"),
                new BigDecimal("37.1"),
                new BigDecimal("127.2"),
                new BigDecimal("37.2")
        );

        assertThat(route).isEmpty();
        server.verify();
    }

    @Test
    void returnsZeroWhenStartAndEndAreTheSamePoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/transit")))
                .andRespond(withSuccess("""
                        { "status": "SAME_POINT" }
                        """, MediaType.APPLICATION_JSON));

        Optional<TransitRoute> route = client.find(
                new BigDecimal("127.1"),
                new BigDecimal("37.1"),
                new BigDecimal("127.1"),
                new BigDecimal("37.1")
        );

        assertThat(route).contains(new TransitRoute(0, 0, List.of()));
        server.verify();
    }
}
