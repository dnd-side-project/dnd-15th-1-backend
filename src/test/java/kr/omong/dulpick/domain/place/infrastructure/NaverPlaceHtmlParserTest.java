package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaverPlaceHtmlParserTest {

    private final NaverPlaceHtmlParser parser = new NaverPlaceHtmlParser();

    @Test
    void extractsPlaceNameAndRoadAddressFromMobilePlaceHtml() {
        String html = """
                <html>
                  <head>
                    <meta content="을지식당 : 네이버&#28;" property="og:title" />
                  </head>
                  <body>
                    <script>
                      window.__APOLLO_STATE__ = {
                        "name":"을지식당",
                        "address":"서울 중구 을지로6가 67-3",
                        "roadAddress":"서울 중구 을지로40길 17"
                      };
                    </script>
                  </body>
                </html>
                """;

        NaverPlaceHtmlParser.ParsedPlace place = parser.parse(html);

        assertThat(place.name()).isEqualTo("을지식당");
        assertThat(place.address()).isEqualTo("서울 중구 을지로40길 17");
    }

    @Test
    void fallsBackToLotNumberAddress() {
        String html = """
                <meta property="og:title" content="을지식당 : 네이버" />
                <script>{"address":"서울 중구 을지로6가 67-3"}</script>
                """;

        assertThat(parser.parse(html).address()).isEqualTo("서울 중구 을지로6가 67-3");
    }

    @Test
    void rejectsHtmlWithoutPlaceNameOrAddress() {
        assertThatThrownBy(() -> parser.parse("<html><title>네이버 지도</title></html>"))
                .isInstanceOf(MetadataUnavailableException.class);
    }
}
