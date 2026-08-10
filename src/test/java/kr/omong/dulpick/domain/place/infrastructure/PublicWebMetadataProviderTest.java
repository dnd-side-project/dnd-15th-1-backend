package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PublicWebMetadataProviderTest {

    private static final String SHORT_URL = "https://naver.me/F1r21MEx";
    private static final String MAP_URL = "https://map.naver.com/p/entry/place/18699959";
    private static final String MOBILE_URL = "https://m.place.naver.com/place/18699959/home";
    private static final String DETAIL_URL = "https://m.place.naver.com/restaurant/18699959/home";

    @Test
    void resolvesNaverShortLinkAndExtractsPlaceFromMobileHtml() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = new PublicWebMetadataProvider(
                properties(),
                Clock.systemUTC(),
                builder,
                new NaverPlaceHtmlParser()
        );
        expectRedirect(server, SHORT_URL, MAP_URL);
        expectRedirect(server, MOBILE_URL, DETAIL_URL);
        server.expect(once(), requestTo(DETAIL_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9"))
                .andRespond(withSuccess("""
                        <html>
                        <head><meta property="og:title" content="을지식당 : 네이버 플레이스"></head>
                        <body><script>{"roadAddress":"서울 중구 을지로40길 17"}</script></body>
                        </html>
                        """, MediaType.TEXT_HTML));

        var metadata = provider.fetch(SHORT_URL, ContentSourceType.NAVER_SHORT_LINK);

        assertThat(metadata.title()).isEqualTo("을지식당");
        assertThat(metadata.caption()).isEqualTo("서울 중구 을지로40길 17");
        server.verify();
    }

    private void expectRedirect(
            MockRestServiceServer server,
            String requestUrl,
            String location
    ) {
        server.expect(once(), requestTo(requestUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, location));
    }

    private PlaceAnalysisProperties properties() {
        return new PlaceAnalysisProperties(
                true,
                100,
                10,
                1,
                true,
                600,
                300,
                3,
                Duration.ofSeconds(5),
                20,
                2
        );
    }
}
