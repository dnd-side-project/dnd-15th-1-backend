package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.RoutePoint;
import kr.omong.dulpick.domain.place.application.TransitLeg;
import kr.omong.dulpick.domain.place.application.TransitRoute;
import kr.omong.dulpick.domain.place.config.KakaoRoutingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoTransitRouteClientTest {

    private static final KakaoRoutingProperties PROPERTIES =
            new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30);

    @Test
    void parsesFirstRouteWithLegsFromKakaoPublicTraffic() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(PROPERTIES, builder);
        server.expect(once(), requestTo(containsString("/v2/routing/publictraffic")))
                .andExpect(header("Authorization", "KakaoAK test-key"))
                .andExpect(queryParam("start_x", "127.0560000"))
                .andExpect(queryParam("start_y", "37.5445000"))
                .andExpect(queryParam("end_x", "127.0410000"))
                .andExpect(queryParam("end_y", "37.5480000"))
                .andRespond(withSuccess("""
                        {
                          "status": "OK",
                          "routes": [
                            {
                              "properties": {
                                "type": "BUS_AND_SUBWAY",
                                "totalDistance": 5200,
                                "totalTime": 2700,
                                "transfers": 1,
                                "fare": {"value": 1550}
                              },
                              "steps": [
                                {
                                  "properties": {
                                    "type": "SUBWAY",
                                    "guidance": "2호선 (성수 > 강남)",
                                    "distance": 4100,
                                    "time": 1800,
                                    "stops": [{"name": "성수"}, {"name": "강남"}],
                                    "vehicles": [{"name": "2호선", "type": "지하철"}]
                                  },
                                  "path": {"points": [[127.056, 37.5445], [127.041, 37.548]]}
                                },
                                {
                                  "properties": {
                                    "type": "WALKING",
                                    "guidance": "도보 5분",
                                    "distance": 300,
                                    "time": 300
                                  },
                                  "path": {"points": [[127.041, 37.548]]}
                                }
                              ]
                            },
                            {"properties": {"type": "BUS", "totalDistance": 1, "totalTime": 1}, "steps": []}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<TransitRoute> route = client.find(
                new BigDecimal("127.0560000"),
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0410000"),
                new BigDecimal("37.5480000")
        );

        assertThat(route).isPresent();
        TransitRoute transit = route.get();
        assertThat(transit.type()).isEqualTo("BUS_AND_SUBWAY");
        assertThat(transit.durationSeconds()).isEqualTo(2700);
        assertThat(transit.transfers()).isEqualTo(1);
        assertThat(transit.fare()).isEqualTo(1550);
        assertThat(transit.legs()).hasSize(2);
        TransitLeg subway = transit.legs().getFirst();
        assertThat(subway.type()).isEqualTo("SUBWAY");
        assertThat(subway.vehicleName()).isEqualTo("2호선");
        assertThat(subway.stops()).containsExactly("성수", "강남");
        assertThat(subway.path()).containsExactly(
                new RoutePoint(37.5445, 127.056),
                new RoutePoint(37.548, 127.041)
        );
        assertThat(transit.legs().get(1).vehicleName()).isNull();
        server.verify();
    }

    @Test
    void returnsEmptyWhenKakaoReturnsNoResults() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(PROPERTIES, builder);
        server.expect(once(), requestTo(containsString("/v2/routing/publictraffic")))
                .andRespond(withSuccess("{\"status\": \"NO_RESULTS\"}", MediaType.APPLICATION_JSON));

        assertThat(client.find(
                new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2")
        )).isEmpty();
        server.verify();
    }

    @Test
    void returnsEmptyWhenKakaoFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(PROPERTIES, builder);
        server.expect(once(), requestTo(containsString("/v2/routing/publictraffic")))
                .andRespond(withServerError());

        assertThat(client.find(
                new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2")
        )).isEmpty();
        server.verify();
    }

    @Test
    void returnsEmptyWithoutCallingKakaoWhenDisabled() {
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(
                new KakaoRoutingProperties(false, "test-key", "https://dapi.kakao.com", 3, 30),
                RestClient.builder()
        );

        assertThat(client.find(
                new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2")
        )).isEmpty();
    }

    @Test
    void stopsCallingKakaoAfterQuotaIsExhausted() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30),
                builder
        );
        server.expect(once(), requestTo(containsString("/v2/routing/publictraffic")))
                .andRespond(withBadRequest()
                        .body("{\"code\":-10,\"msg\":\"API limit has been exceeded.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        Optional<?> first = client.find(
                new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2")
        );
        Optional<?> second = client.find(
                new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2")
        );

        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        server.verify();
    }

    @Test
    void keepsCallingKakaoAfterNonQuotaClientError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoTransitRouteClient client = new KakaoTransitRouteClient(
                new KakaoRoutingProperties(true, "test-key", "https://dapi.kakao.com", 3, 30),
                builder
        );
        server.expect(org.springframework.test.web.client.ExpectedCount.times(2),
                        requestTo(containsString("/v2/routing/publictraffic")))
                .andRespond(withBadRequest()
                        .body("{\"code\":-2,\"msg\":\"invalid request\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        client.find(new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2"));
        client.find(new BigDecimal("127.1"), new BigDecimal("37.1"),
                new BigDecimal("127.2"), new BigDecimal("37.2"));

        server.verify();
    }
}
