package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.config.InstagramProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PublicInstagramMetadataProviderTest {

    private static final String REEL_URL = "https://www.instagram.com/reel/example";

    @Test
    void extractsInstagramMetadataWithoutFollowingRedirectsAutomatically() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicInstagramMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(REEL_URL))
                .andRespond(withSuccess("""
                        <meta property="og:title" content="둘픽 on Instagram: &quot;성수 카페 모음&quot;">
                        <meta property="og:description" content="12 likes, 3 comments - dulpick on August 10, 2026: &quot;성수 카페 모음\n본문&quot;.">
                        <meta property="og:image" content="https://cdn.example.com/cover.jpg">
                        """, MediaType.TEXT_HTML));

        var metadata = provider.fetch(REEL_URL, ContentSourceType.INSTAGRAM_REEL);

        assertThat(metadata.sourceAuthorUsername()).isEqualTo("dulpick");
        assertThat(metadata.title()).isEqualTo("성수 카페 모음");
        assertThat(metadata.caption()).isEqualTo("본문");
        assertThat(metadata.likeCount()).isEqualTo(12L);
        assertThat(metadata.commentCount()).isEqualTo(3L);
        server.verify();
    }

    @Test
    void rejectsRedirectToDisallowedHost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicInstagramMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(REEL_URL))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, "https://example.com/private"));

        assertThatThrownBy(() -> provider.fetch(REEL_URL, ContentSourceType.INSTAGRAM_REEL))
                .isInstanceOf(MetadataUnavailableException.class);

        server.verify();
    }

    @Test
    void rejectsHtmlThatExceedsStreamingLimit() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicInstagramMetadataProvider provider = provider(builder);
        server.expect(once(), requestTo(REEL_URL))
                .andRespond(withSuccess("x".repeat(1_000_001), MediaType.TEXT_HTML));

        assertThatThrownBy(() -> provider.fetch(REEL_URL, ContentSourceType.INSTAGRAM_REEL))
                .isInstanceOf(MetadataUnavailableException.class);

        server.verify();
    }

    private PublicInstagramMetadataProvider provider(RestClient.Builder builder) {
        HostAddressResolver resolver = host -> List.of(publicAddress());
        return new PublicInstagramMetadataProvider(
                properties(),
                Clock.fixed(Instant.parse("2026-08-10T03:00:00Z"), ZoneOffset.UTC),
                new PublicWebUrlValidator(resolver),
                builder
        );
    }

    private InstagramProperties properties() {
        return new InstagramProperties(
                false,
                null,
                null,
                null,
                true,
                5
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
