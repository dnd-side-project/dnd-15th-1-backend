package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.config.KakaoMapPhotoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoMapPhotoProviderTest {

    @Test
    void returnsLimitedTrustedPhotosAsHttps() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapPhotoProvider provider = new KakaoMapPhotoProvider(
                properties(true, 2),
                builder
        );
        server.expect(once(), queryParam("page", "1"))
                .andExpect(header("appVersion", "6.6.0"))
                .andExpect(header("pf", "PC"))
                .andRespond(withSuccess("""
                        {
                          "photos": [
                            {"url": "http://t1.kakaocdn.net/mystore/first"},
                            {"url": "https://postfiles.pstatic.net/allowed"},
                            {"url": "//img1.daumcdn.net/second"},
                            {"url": "https://images.example.com/excluded"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findImageUrls("610012827")).containsExactly(
                "https://t1.kakaocdn.net/mystore/first",
                "https://postfiles.pstatic.net/allowed"
        );
        server.verify();
    }

    @Test
    void returnsEmptyWhenKakaoMapPhotoRequestFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapPhotoProvider provider = new KakaoMapPhotoProvider(
                properties(true, 5),
                builder
        );
        server.expect(once(), queryParam("page", "1"))
                .andRespond(withServerError());

        assertThat(provider.findImageUrls("610012827")).isEmpty();
        server.verify();
    }

    @Test
    void retriesPhotoRequestDuringInitialFetch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapPhotoProvider provider = new KakaoMapPhotoProvider(
                properties(true, 5, 3, 0),
                builder
        );
        server.expect(once(), queryParam("page", "1"))
                .andRespond(withServerError());
        server.expect(once(), queryParam("page", "1"))
                .andRespond(withSuccess("""
                        {"photos": [{"url": "https://t1.kakaocdn.net/recovered"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findImageUrls("610012827"))
                .containsExactly("https://t1.kakaocdn.net/recovered");
        server.verify();
    }

    @Test
    void acceptsDthumbPstaticPhotosFromKakao() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapPhotoProvider provider = new KakaoMapPhotoProvider(
                properties(true, 5),
                builder
        );
        server.expect(once(), queryParam("page", "1"))
                .andRespond(withSuccess("""
                        {"photos": [{"url": "https://dthumb-phinf.pstatic.net/example"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findImageUrls("610012827"))
                .containsExactly("https://dthumb-phinf.pstatic.net/example");
        server.verify();
    }

    @Test
    void acceptsKakaoRoadviewPhotos() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapPhotoProvider provider = new KakaoMapPhotoProvider(
                properties(true, 5),
                builder
        );
        server.expect(once(), queryParam("page", "1"))
                .andRespond(withSuccess("""
                        {"photos": [{"url": "https://map.kakaocdn.net/map_roadview/2025/06/9148120/right_800.jpg"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findImageUrls("610012827"))
                .containsExactly("https://map.kakaocdn.net/map_roadview/2025/06/9148120/right_800.jpg");
        server.verify();
    }

    @Test
    void skipsRequestWhenScrapingIsDisabledOrPlaceIdIsInvalid() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapPhotoProvider provider = new KakaoMapPhotoProvider(
                properties(false, 5),
                builder
        );

        assertThat(provider.findImageUrls("610012827")).isEmpty();
        assertThat(provider.findImageUrls("../../internal")).isEmpty();
        server.verify();
    }

    private KakaoMapPhotoProperties properties(boolean enabled, int maxImages) {
        return new KakaoMapPhotoProperties(
                enabled,
                "https://place-api.map.kakao.com",
                2,
                maxImages,
                "6.6.0",
                1,
                0
        );
    }

    private KakaoMapPhotoProperties properties(
            boolean enabled,
            int maxImages,
            int retryAttempts,
            long retryDelayMillis
    ) {
        return new KakaoMapPhotoProperties(
                enabled,
                "https://place-api.map.kakao.com",
                2,
                maxImages,
                "6.6.0",
                retryAttempts,
                retryDelayMillis
        );
    }
}
