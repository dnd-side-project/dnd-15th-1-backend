package kr.omong.dulpick.global.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTimeTest {

    @Test
    void convertsInstantToKoreaStandardTime() {
        Instant instant = Instant.parse("2026-07-31T07:02:50.107591Z");

        LocalDateTime koreaTime = ServiceTime.toLocalDateTime(instant);

        assertThat(koreaTime)
                .isEqualTo(LocalDateTime.parse("2026-07-31T16:02:50.107591"));
    }

    @Test
    void keepsNullableTimeAsNull() {
        assertThat(ServiceTime.toLocalDateTime(null)).isNull();
    }
}
