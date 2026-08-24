package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentThumbnailDownloader;
import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InstagramThumbnailDownloaderTest {

    private static final String IMAGE_URL = "https://scontent-icn2-1.cdninstagram.com/image.jpg?token=signed";

    @Test
    void downloadsInstagramCdnImageWithBrowserHeaders() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InstagramThumbnailDownloader downloader = downloader(builder);
        server.expect(once(), requestTo(IMAGE_URL))
                .andExpect(header(HttpHeaders.REFERER, "https://www.instagram.com/"))
                .andRespond(withSuccess("image-bytes", MediaType.IMAGE_JPEG));

        ContentThumbnailDownloader.DownloadedThumbnail result = downloader.download(IMAGE_URL);

        assertThat(result.bytes()).containsExactly("image-bytes".getBytes());
        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_JPEG);
        server.verify();
    }

    @Test
    void rejectsUpstreamForbiddenResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InstagramThumbnailDownloader downloader = downloader(builder);
        server.expect(once(), requestTo(IMAGE_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> downloader.download(IMAGE_URL))
                .isInstanceOf(PublicContentImageUnavailableException.class);
    }

    @Test
    void rejectsNonInstagramImageHostBeforeRequest() {
        InstagramThumbnailDownloader downloader = downloader(RestClient.builder());

        assertThatThrownBy(() -> downloader.download("https://example.com/image.jpg"))
                .isInstanceOf(PublicContentImageUnavailableException.class);
    }

    private InstagramThumbnailDownloader downloader(RestClient.Builder builder) {
        return new InstagramThumbnailDownloader(
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
