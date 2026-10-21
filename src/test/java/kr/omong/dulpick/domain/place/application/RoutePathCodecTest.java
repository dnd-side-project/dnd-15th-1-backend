package kr.omong.dulpick.domain.place.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutePathCodecTest {

    @Test
    void roundTripsRoutePoints() {
        List<RoutePoint> path = List.of(new RoutePoint(37.5445, 127.056), new RoutePoint(37.548, 127.041));

        String encoded = RoutePathCodec.encode(path);

        assertThat(encoded).isEqualTo("37.5445,127.056;37.548,127.041");
        assertThat(RoutePathCodec.decode(encoded)).isEqualTo(path);
    }

    @Test
    void encodesEmptyPathAsNullAndDecodesBlankAsEmpty() {
        assertThat(RoutePathCodec.encode(List.of())).isNull();
        assertThat(RoutePathCodec.encode(null)).isNull();
        assertThat(RoutePathCodec.decode(null)).isEmpty();
        assertThat(RoutePathCodec.decode("")).isEmpty();
    }

    @Test
    void decodesMalformedPathAsEmpty() {
        assertThat(RoutePathCodec.decode("37.5,127.0;broken")).isEmpty();
        assertThat(RoutePathCodec.decode("a,b")).isEmpty();
    }
}
