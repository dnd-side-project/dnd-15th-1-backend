package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaverPlaceHtmlParserTest {

    private final NaverPlaceHtmlParser parser = new NaverPlaceHtmlParser(new ObjectMapper());

    @Test
    void extractsPlaceFromJsonLdAsOneObject() {
        String html = """
                <script type="application/ld+json">
                  {
                    "@context":"https://schema.org",
                    "@type":"Restaurant",
                    "name":"을지식당",
                    "address": {
                      "@type":"PostalAddress",
                      "addressRegion":"서울",
                      "addressLocality":"중구",
                      "streetAddress":"을지로40길 17"
                    }
                  }
                </script>
                """;

        NaverPlaceHtmlParser.ParsedPlace place = parser.parse(html);

        assertThat(place.name()).isEqualTo("을지식당");
        assertThat(place.address()).isEqualTo("서울 중구 을지로40길 17");
    }

    @Test
    void decodesEscapedInitialStateJson() {
        String html = """
                <script>
                  window.__STATE__ = {
                    "place": {
                      "name":"\\uC744\\uC9C0\\uC2DD\\uB2F9",
                      "roadAddress":"\\uC11C\\uC6B8 \\uC911\\uAD6C \\uC744\\uC9C0\\uB85C40\\uAE38 17\\/1"
                    }
                  };
                </script>
                """;

        NaverPlaceHtmlParser.ParsedPlace place = parser.parse(html);

        assertThat(place.name()).isEqualTo("을지식당");
        assertThat(place.address()).isEqualTo("서울 중구 을지로40길 17/1");
    }

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
