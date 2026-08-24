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
    private static final String KAKAO_PLACE_URL = "https://place.map.kakao.com/1928046415";
    private static final String KAKAO_SHORT_URL = "https://kko.to/hltcaU_mqV";
    private static final String KAKAO_APP_LINK_URL = "https://applink.map.kakao.com/place?id=1402324982&t_src=share";
    private static final String KAKAO_SHORT_PLACE_URL = "https://place.map.kakao.com/1402324982";

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
    void extractsKakaoPlaceMetadataFromDirectPlaceLink() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(KAKAO_PLACE_URL))
                .andRespond(withSuccess("""
                        <meta property="og:title" content="카카오프렌즈 스타필드코엑스몰">
                        <meta property="og:description" content="서울 강남구 삼성동 159-1 지하1층">
                        """, MediaType.TEXT_HTML));

        var metadata = provider.fetch(KAKAO_PLACE_URL, ContentSourceType.KAKAO_MAP);

        assertThat(metadata.title()).isEqualTo("카카오프렌즈 스타필드코엑스몰");
        assertThat(metadata.caption()).isEqualTo("서울 강남구 삼성동 159-1 지하1층");
        server.verify();
    }

    @Test
    void followsKakaoShortLinkToPlaceMetadata() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        expectRedirect(server, KAKAO_SHORT_URL, KAKAO_APP_LINK_URL);
        server.expect(once(), requestTo(KAKAO_APP_LINK_URL))
                .andRespond(withSuccess("<html><title>카카오맵</title></html>", MediaType.TEXT_HTML));
        server.expect(once(), requestTo(KAKAO_SHORT_PLACE_URL))
                .andRespond(withSuccess("""
                        <meta property="og:title" content="스타벅스 산본사거리점 | 카카오맵">
                        <meta property="og:description" content="경기 군포시 고산로 701">
                        """, MediaType.TEXT_HTML));

        var metadata = provider.fetch(KAKAO_SHORT_URL, ContentSourceType.KAKAO_MAP);

        assertThat(metadata.title()).isEqualTo("스타벅스 산본사거리점");
        assertThat(metadata.caption()).isEqualTo("경기 군포시 고산로 701");
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
    void rejectsKakaoShortLinkRedirectToDisallowedHost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicWebMetadataProvider provider = provider(builder);
        expectRedirect(server, KAKAO_SHORT_URL, "https://example.com/place/1");

        assertThatThrownBy(() -> provider.fetch(KAKAO_SHORT_URL, ContentSourceType.KAKAO_MAP))
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
                2,
                3
        );
    }
}
