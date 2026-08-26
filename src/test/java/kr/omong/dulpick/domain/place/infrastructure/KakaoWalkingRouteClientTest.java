package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.WalkingRoute;
import kr.omong.dulpick.domain.place.config.KakaoRoutingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoWalkingRouteClientTest {

    @Test
    void returnsDistanceAndDurationFromKakaoWalkRoute() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoWalkingRouteClient client = new KakaoWalkingRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/walk")))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("start_x", "127.0560000"))
                .andExpect(queryParam("start_y", "37.5445000"))
                .andExpect(queryParam("end_x", "127.0410000"))
                .andExpect(queryParam("end_y", "37.5480000"))
                .andExpect(queryParam("route_mode", "SHORTEST"))
                .andRespond(withSuccess("""
                        {
                          "status": "OK",
                          "route": {
                            "properties": {
                              "totalDistance": 4025,
                              "totalTime": 3914
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<WalkingRoute> route = client.find(
                new BigDecimal("127.0560000"),
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0410000"),
                new BigDecimal("37.5480000")
        );

        assertThat(route).contains(new WalkingRoute(4025, 3914));
        server.verify();
    }

    @Test
    void returnsEmptyWhenKakaoWalkRouteFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoWalkingRouteClient client = new KakaoWalkingRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/walk")))
                .andRespond(withServerError());

        Optional<WalkingRoute> route = client.find(
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
        KakaoWalkingRouteClient client = new KakaoWalkingRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/walk")))
                .andRespond(withSuccess("""
                        { "status": "SAME_POINT" }
                        """, MediaType.APPLICATION_JSON));

        Optional<WalkingRoute> route = client.find(
                new BigDecimal("127.1"),
                new BigDecimal("37.1"),
                new BigDecimal("127.1"),
                new BigDecimal("37.1")
        );

        assertThat(route).contains(new WalkingRoute(0, 0));
        server.verify();
    }
}
