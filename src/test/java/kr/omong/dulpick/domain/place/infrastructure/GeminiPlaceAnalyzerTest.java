package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.application.ContentThumbnailDownloader;
import kr.omong.dulpick.domain.place.config.GeminiProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiPlaceAnalyzerTest {

    @Test
    void doesNotDownloadImagesWhenCaptionAlreadyProvidesAPlace() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        GeminiProperties properties = new GeminiProperties(
                true, "test-key", "gemini-test", "https://generativelanguage.googleapis.com", 3
        );
        GeminiPlaceAnalyzer analyzer = new GeminiPlaceAnalyzer(
                properties, new ObjectMapper(), downloader, builder
        );
        server.expect(once(), requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"
                ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("inlineData")
                )))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\\"candidates\\\":[{\\\"name\\\":\\\"서울숲 카페\\\"}]}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        var result = analyzer.analyze(new ContentMetadata(
                "https://www.instagram.com/p/example",
                ContentSourceType.INSTAGRAM_POST,
                "서울숲 카페 추천",
                "서울숲 카페에 다녀왔어요",
                "https://cdninstagram.com/place.jpg",
                "hash",
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("서울숲 카페");
        verify(downloader, never()).download(any());
        server.verify();
    }

    @Test
    void sendsAUsableInstagramImageToGeminiForVisualTextExtraction() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        when(downloader.download("https://cdninstagram.com/place.jpg"))
                .thenReturn(new ContentThumbnailDownloader.DownloadedThumbnail(
                        new byte[]{1, 2, 3}, MediaType.IMAGE_JPEG
                ));
        GeminiProperties properties = new GeminiProperties(
                true, "test-key", "gemini-test", "https://generativelanguage.googleapis.com", 3
        );
        GeminiPlaceAnalyzer analyzer = new GeminiPlaceAnalyzer(
                properties, new ObjectMapper(), downloader, builder
        );
        server.expect(once(), requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"
                ))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("inlineData")
                )))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\\"candidates\\\":[]}"}]}}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"
                ))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inlineData")))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\\"candidates\\\":[]}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        var result = analyzer.analyze(new ContentMetadata(
                "https://www.instagram.com/p/example",
                ContentSourceType.INSTAGRAM_POST,
                "카페",
                "본문",
                "https://cdninstagram.com/place.jpg",
                "hash",
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(result).isEmpty();
        server.verify();
    }
}
