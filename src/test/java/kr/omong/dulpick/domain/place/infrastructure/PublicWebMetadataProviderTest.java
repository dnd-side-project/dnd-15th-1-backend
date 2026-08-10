package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        PublicWebMetadataProvider provider = provider(builder);
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

    @Test
    void followsNaverBlogMainFrameAndExtractsMetadata() {
        String blogUrl = "https://blog.naver.com/dulpick/123";
        String frameUrl = "https://blog.naver.com/PostView.naver?blogId=dulpick&logNo=123";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(blogUrl))
                .andRespond(withSuccess("""
                        <iframe id="mainFrame" src="/PostView.naver?blogId=dulpick&amp;logNo=123"></iframe>
                        """, MediaType.TEXT_HTML));
        server.expect(once(), requestTo(frameUrl))
                .andRespond(withSuccess("""
                        <meta property="og:title" content="성수 카페 기록">
                        <meta property="og:description" content="서울 성동구 카페 세 곳">
                        """, MediaType.TEXT_HTML));

        var metadata = provider.fetch(blogUrl, ContentSourceType.NAVER_BLOG);

        assertThat(metadata.title()).isEqualTo("성수 카페 기록");
        assertThat(metadata.caption()).isEqualTo("서울 성동구 카페 세 곳");
        server.verify();
    }

    @Test
    void extractsTistoryMetadataWithoutChangingExistingFlow() {
        String url = "https://sample.tistory.com/entry/place";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(url))
                .andRespond(withSuccess("""
                        <meta property="og:title" content="부산 여행 장소">
                        <meta property="og:description" content="해운대에서 찾은 장소">
                        """, MediaType.TEXT_HTML));

        var metadata = provider.fetch(url, ContentSourceType.TISTORY);

        assertThat(metadata.title()).isEqualTo("부산 여행 장소");
        assertThat(metadata.caption()).isEqualTo("해운대에서 찾은 장소");
        server.verify();
    }

    @Test
    void rejectsRedirectToDisallowedHost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        expectRedirect(server, SHORT_URL, "https://example.com/private");

        assertThatThrownBy(() -> provider.fetch(SHORT_URL, ContentSourceType.NAVER_SHORT_LINK))
                .isInstanceOf(MetadataUnavailableException.class);

        server.verify();
    }

    @Test
    void rejectsHtmlFromContentLengthBeforeReadingBody() {
        String url = "https://sample.tistory.com/entry/place";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(url))
                .andRespond(withSuccess("small", MediaType.TEXT_HTML)
                        .header(HttpHeaders.CONTENT_LENGTH, "1000001"));

        assertThatThrownBy(() -> provider.fetch(url, ContentSourceType.TISTORY))
                .isInstanceOf(MetadataUnavailableException.class);

        server.verify();
    }

    @Test
    void rejectsHtmlThatExceedsStreamingLimit() {
        String url = "https://sample.tistory.com/entry/place";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(url))
                .andRespond(withSuccess("x".repeat(1_000_001), MediaType.TEXT_HTML));

        assertThatThrownBy(() -> provider.fetch(url, ContentSourceType.TISTORY))
                .isInstanceOf(MetadataUnavailableException.class);

        server.verify();
    }

    private PublicWebMetadataProvider provider(RestClient.Builder builder) {
        HostAddressResolver resolver = host -> List.of(publicAddress());
        return new PublicWebMetadataProvider(
                properties(),
                Clock.systemUTC(),
                builder,
                new NaverPlaceHtmlParser(new ObjectMapper()),
                new PublicWebUrlValidator(resolver)
        );
    }

    private InetAddress publicAddress() {
        try {
            return InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
