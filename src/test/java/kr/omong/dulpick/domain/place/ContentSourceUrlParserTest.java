package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.application.ContentSourceUrlParser;
import kr.omong.dulpick.domain.place.application.exception.UnsupportedSourceUrlException;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentSourceUrlParserTest {

    private final ContentSourceUrlParser parser = new ContentSourceUrlParser();

    @Test
    void removesInstagramTrackingQueryFromReelUrl() {
        ContentSourceUrlParser.ParsedSource source = parser.parse(
                "https://www.instagram.com/reel/DbS9Ne4tlq9/?igsh=eDQ1YW54cGozOHVy"
        );

        assertThat(source.sourceType()).isEqualTo(ContentSourceType.INSTAGRAM_REEL);
        assertThat(source.canonicalUrl())
                .isEqualTo("https://www.instagram.com/reel/DbS9Ne4tlq9");
    }

    @Test
    void removesImageIndexAndTrackingQueryFromPostUrl() {
        ContentSourceUrlParser.ParsedSource source = parser.parse(
                "https://www.instagram.com/p/DazhCMqk0kn/?img_index=2&igsh=ZWNkaWFlZGxjNjE1"
        );

        assertThat(source.sourceType()).isEqualTo(ContentSourceType.INSTAGRAM_POST);
        assertThat(source.canonicalUrl())
                .isEqualTo("https://www.instagram.com/p/DazhCMqk0kn");
    }

    @Test
    void rejectsYoutubeShortsForCurrentScope() {
        assertThatThrownBy(() -> parser.parse("https://youtube.com/shorts/example"))
                .isInstanceOf(UnsupportedSourceUrlException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.instagram.com/p/DbuPwBvgGZ5/?igsh=MWw0ODhwZGJjNzNleg==",
            "https://www.instagram.com/reel/DbS9Ne4tlq9/?igsh=eDQ1YW54cGozOHVy",
            "https://www.instagram.com/reel/Da7nneChLGX/?igsh=OTlodHd5MzJkYjcx",
            "https://www.instagram.com/reel/DaP3I2CAUuj/?igsh=MXFsNXZybjdnOWhhNQ==",
            "https://www.instagram.com/p/DbxYpnsk1Qm/?igsh=bHNpbHFsN2d2OGV2",
            "https://www.instagram.com/p/DbxR-YYEuuy/?igsh=b2hqYmtzemVmZnlm"
    })
    void acceptsProvidedInstagramExamples(String url) {
        assertThat(parser.parse(url).canonicalUrl())
                .doesNotContain("?")
                .doesNotContain("igsh");
    }
}
