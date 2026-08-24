package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentThumbnailDownloader;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoMapImageDownloaderTest {

    private static final String KAKAO_IMAGE_URL = "https://t1.kakaocdn.net/place/image.jpg";
    private static final String NAVER_IMAGE_URL = "https://postfiles.pstatic.net/place/image.jpg";

    @Test
    void sendsKakaoRefererToKakaoCdn() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapImageDownloader downloader = downloader(builder);
        server.expect(once(), requestTo(KAKAO_IMAGE_URL))
                .andExpect(header(HttpHeaders.REFERER, "https://place.map.kakao.com/"))
                .andRespond(withSuccess("image-bytes", MediaType.IMAGE_JPEG));

        ContentThumbnailDownloader.DownloadedThumbnail result = downloader.download(KAKAO_IMAGE_URL);

        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_JPEG);
        server.verify();
    }

    @Test
    void omitsKakaoRefererForNaverCdn() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoMapImageDownloader downloader = downloader(builder);
        server.expect(once(), requestTo(NAVER_IMAGE_URL))
                .andExpect(headerDoesNotExist(HttpHeaders.REFERER))
                .andRespond(withSuccess("image-bytes", MediaType.IMAGE_JPEG));

        ContentThumbnailDownloader.DownloadedThumbnail result = downloader.download(NAVER_IMAGE_URL);

        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_JPEG);
        server.verify();
    }

    private KakaoMapImageDownloader downloader(RestClient.Builder builder) {
        return new KakaoMapImageDownloader(
                new ContentThumbnailProperties(
                        "http://localhost:8080",
                        "./build/test-content-images",
                        5,
                        5_000_000L
                ),
                host -> List.of(publicAddress()),
                builder
        );
    }

    private InetAddress publicAddress() {
        try {
            return InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
