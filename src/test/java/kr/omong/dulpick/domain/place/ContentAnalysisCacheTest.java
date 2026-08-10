package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ContentAnalysisCacheTest {

    @Test
    void storesAnalysisVersionAndContentHashSeparatelyFromPublishedMetadata() {
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        Content content = Content.create(
                "https://www.instagram.com/p/example",
                "url-hash",
                ContentSourceType.INSTAGRAM_POST,
                "제목",
                "내용",
                null,
                "published-hash",
                now
        );

        content.updateExtractedAnalysis(
                "analysis-hash",
                "gemini-3.5-flash-lite",
                "place-extraction-v3",
                "[]",
                now.plusSeconds(1)
        );

        assertThat(content.getAnalysisContentHash()).isEqualTo("analysis-hash");
        assertThat(content.getAnalyzerModel()).isEqualTo("gemini-3.5-flash-lite");
        assertThat(content.getPromptVersion()).isEqualTo("place-extraction-v3");
        assertThat(content.getExtractedCandidatesJson()).isEqualTo("[]");
        assertThat(content.getAnalyzedAt()).isEqualTo(now.plusSeconds(1));
    }
}
