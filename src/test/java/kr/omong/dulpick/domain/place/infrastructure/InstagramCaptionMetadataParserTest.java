package kr.omong.dulpick.domain.place.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstagramCaptionMetadataParserTest {

    @Test
    void separatesCaptionAndEngagementMetadata() {
        InstagramCaptionMetadataParser.Parsed parsed = InstagramCaptionMetadataParser.parse(
                "찐 on Instagram: \"망원동 빵지순례 스팟이 또 생겼어요•••🍞🚨 *광고\"",
                "1,833 likes, 76 comments - jjin_.record on August 7, 2026: "
                        + "\"망원동 빵지순례 스팟이 또 생겼어요•••🍞🚨 *광고\n\n"
                        + "▪️ 망원 카페 - 밀빛 (@millbit_seoul)\".",
                ""
        );

        assertThat(parsed.displayName()).isEqualTo("찐");
        assertThat(parsed.username()).isEqualTo("jjin_.record");
        assertThat(parsed.likeCount()).isEqualTo(1_833L);
        assertThat(parsed.commentCount()).isEqualTo(76L);
        assertThat(parsed.title()).isEqualTo("망원동 빵지순례 스팟이 또 생겼어요•••🍞🚨 *광고");
        assertThat(parsed.content()).contains("망원 카페 - 밀빛");
    }

    @Test
    void parsesAbbreviatedEngagementCounts() {
        InstagramCaptionMetadataParser.Parsed parsed = InstagramCaptionMetadataParser.parse(
                "작성자 on Instagram: \"제목\"",
                "12K likes, 1.2M comments - account on August 7, 2026: \"제목\"",
                ""
        );

        assertThat(parsed.likeCount()).isEqualTo(12_000L);
        assertThat(parsed.commentCount()).isEqualTo(1_200_000L);
    }
}
