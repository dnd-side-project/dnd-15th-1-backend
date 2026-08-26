package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.KakaoProperties;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoPlaceSearchClientCacheTest {

    private static final String DOCUMENTS_RESPONSE = """
            {
              "documents": [
                {
                  "id": "12345",
                  "place_name": "테스트 카페",
                  "address_name": "서울 강남구",
                  "road_address_name": "서울 강남구 테헤란로",
                  "y": "37.5",
                  "x": "127.0",
                  "category_group_code": "CE7",
                  "category_name": "음식점 > 카페",
                  "phone": "02-000-0000",
                  "place_url": "http://place.map.kakao.com/12345"
                }
              ],
              "meta": {"is_end": true}
            }
            """;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-26T00:00:00Z"));

    @Test
    void servesRepeatedKeywordQueriesFromCache() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
                properties(), builder, clock
        );
        server.expect(once(), anything())
                .andRespond(withSuccess(DOCUMENTS_RESPONSE, MediaType.APPLICATION_JSON));

        List<PlaceSearchResult> first = client.search("테스트 카페");
        List<PlaceSearchResult> second = client.search("테스트 카페");

        assertThat(first).hasSize(1);
        assertThat(first).isSameAs(second);
        server.verify();
    }

    @Test
    void refetchesKeywordQueryAfterCacheTtlExpires() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
                properties(), builder, clock
        );
        server.expect(once(), anything())
                .andRespond(withSuccess(DOCUMENTS_RESPONSE, MediaType.APPLICATION_JSON));
        server.expect(once(), anything())
                .andRespond(withSuccess(DOCUMENTS_RESPONSE, MediaType.APPLICATION_JSON));

        client.search("테스트 카페");
        clock.advance(Duration.ofMinutes(11));
        client.search("테스트 카페");

        server.verify();
    }

    @Test
    void releasesSearchPermitWhenRequestFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
                properties(), builder, clock, 1
        );
        server.expect(once(), anything())
                .andRespond(withServerError());
        server.expect(once(), anything())
                .andRespond(withSuccess(DOCUMENTS_RESPONSE, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("테스트 카페"))
                .isInstanceOf(PlaceVerificationUnavailableException.class);
        assertThat(client.search("테스트 카페")).hasSize(1);

        server.verify();
    }

    @Test
    void sizesSearchPermitsAboveVerificationConcurrency() {
        KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
                properties(),
                clock,
                new PlaceAnalysisProperties(
                        true, 100, 10, 1, true, 600, 300, 3,
                        Duration.ofSeconds(5), 20, 8, 20
                )
        );

        assertThat(client.searchPermitCount()).isEqualTo(24);

        KakaoPlaceSearchClient defaultClient = new KakaoPlaceSearchClient(
                properties(),
                clock,
                new PlaceAnalysisProperties(
                        true, 100, 10, 1, true, 600, 300, 3,
                        Duration.ofSeconds(5), 20, 8, 12
                )
        );

        assertThat(defaultClient.searchPermitCount()).isEqualTo(16);
    }

    @Test
    void collapsesConcurrentIdenticalQueriesIntoSingleRequest() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
                properties(), builder, clock
        );
        server.expect(once(), anything())
                .andRespond(withSuccess(DOCUMENTS_RESPONSE, MediaType.APPLICATION_JSON));
        int callerCount = 6;
        CyclicBarrier barrier = new CyclicBarrier(callerCount);
        List<Future<List<PlaceSearchResult>>> callers = new java.util.ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(callerCount);
        for (int index = 0; index < callerCount; index++) {
            callers.add(executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return client.search("테스트 카페");
            }));
        }

        List<PlaceSearchResult> first = callers.getFirst().get(5, TimeUnit.SECONDS);
        for (Future<List<PlaceSearchResult>> caller : callers) {
            assertThat(caller.get(5, TimeUnit.SECONDS)).isSameAs(first);
        }
        executor.shutdownNow();

        server.verify();
    }

    @Test
    void doesNotCacheEmptyKeywordResponses() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceSearchClient client = new KakaoPlaceSearchClient(
                properties(), builder, clock
        );
        server.expect(once(), anything())
                .andRespond(withSuccess("{\"documents\": [], \"meta\": {\"is_end\": true}}", MediaType.APPLICATION_JSON));
        server.expect(once(), anything())
                .andRespond(withSuccess(DOCUMENTS_RESPONSE, MediaType.APPLICATION_JSON));

        assertThat(client.search("테스트 카페")).isEmpty();
        clock.advance(Duration.ofSeconds(1));
        assertThat(client.search("테스트 카페")).hasSize(1);

        server.verify();
    }

    private KakaoProperties properties() {
        return new KakaoProperties(true, "local-test-key", "https://dapi.kakao.com", 3);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
