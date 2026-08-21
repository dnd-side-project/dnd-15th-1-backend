package kr.omong.dulpick.global.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Test
    void usesMidnightWhenScheduledTimeIsMissing() {
        Instant scheduledAt = ServiceTime.toScheduledInstant(
                LocalDate.of(2026, 8, 30),
                null
        );

        assertThat(ServiceTime.toLocalDateTime(scheduledAt))
                .isEqualTo(LocalDateTime.of(2026, 8, 30, 0, 0));
        assertThat(ServiceTime.toResponseTime(ServiceTime.toLocalDateTime(scheduledAt)))
                .isNull();
    }

    @Test
    void keepsExplicitMidnightAsNullInResponseTime() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 8, 30, 0, 0);

        assertThat(ServiceTime.toResponseTime(scheduledAt)).isNull();
    }

    @Test
    void keepsExplicitTimeInResponseTime() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 8, 30, 19, 30);

        assertThat(ServiceTime.toResponseTime(scheduledAt)).isEqualTo(LocalTime.of(19, 30));
    }
}
