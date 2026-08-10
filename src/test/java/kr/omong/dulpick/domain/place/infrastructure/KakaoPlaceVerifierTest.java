package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.config.KakaoProperties;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoPlaceVerifierTest {

    @Test
    void capsFallbackSearchesPerCandidate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoProperties properties = new KakaoProperties(true, "test-key", "https://dapi.kakao.com", 3);
        KakaoPlaceSearchClient searchClient = new KakaoPlaceSearchClient(properties, builder);
        KakaoPlaceVerifier verifier = new KakaoPlaceVerifier(properties, searchClient, new KakaoPlaceMatcher());
        String name = "하나 둘 셋 넷 다섯 여섯 일곱 여덟";
        expectEmpty(server, name);
        expectEmpty(server, "하나 둘 셋 넷 다섯 여섯 일곱");
        expectEmpty(server, "하나 둘 셋 넷 다섯 여섯");
        expectEmpty(server, "하나 둘 셋 넷 다섯");
        expectEmpty(server, "하나 둘 셋 넷");
        expectEmpty(server, "하나 둘 셋");

        assertThat(verifier.verify(new ExtractedPlace(name, null, null, "INFERRED"))).isNull();

        server.verify();
    }

    @Test
    void searchesPreciseAddressWithoutNameAndReturnsChangedNameForReview() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoPlaceSearchClient searchClient = new KakaoPlaceSearchClient(
                new KakaoProperties(true, "test-key", "https://dapi.kakao.com", 3),
                builder
        );
        KakaoPlaceVerifier verifier = new KakaoPlaceVerifier(
                new KakaoProperties(true, "test-key", "https://dapi.kakao.com", 3),
                searchClient,
                new KakaoPlaceMatcher()
        );
        expectEmpty(server, "시어풀빌라 경기 가평군 설악면 유명로 100");
        expectEmpty(server, "시어풀빌라 경기 가평군");
        expectEmpty(server, "시어풀빌라");
        server.expect(once(), queryParam("query", encoded("경기 가평군 설악면 유명로 100")))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "id": "100",
                            "place_name": "시어팬션",
                            "address_name": "경기 가평군 설악면 선촌리 10",
                            "road_address_name": "경기 가평군 설악면 유명로 100",
                            "y": "37.1",
                            "x": "127.1",
                            "category_group_code": "AD5",
                            "category_name": "여행 > 숙박 > 펜션"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = verifier.verify(new ExtractedPlace(
                "시어풀빌라",
                "경기 가평군 설악면 유명로 100",
                "시어풀빌라 경기 가평군 설악면 유명로 100",
                "EXPLICIT_VENUE"
        ));

        assertThat(result.place().name()).isEqualTo("시어팬션");
        assertThat(result.status()).isEqualTo(PlaceVerificationStatus.REVIEW_REQUIRED);
        server.verify();
    }

    private void expectEmpty(MockRestServiceServer server, String query) {
        server.expect(once(), queryParam("query", encoded(query)))
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));
    }

    private String encoded(String query) {
        return UriUtils.encodeQueryParam(query, StandardCharsets.UTF_8);
    }
}
