package kr.omong.dulpick.global.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class ServiceTime {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private ServiceTime() {
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZONE_ID);
    }

    public static Instant toScheduledInstant(LocalDate date, LocalTime time) {
        if (date == null) {
            return null;
        }
        LocalTime resolvedTime = time == null ? LocalTime.MIDNIGHT : time;
        return LocalDateTime.of(date, resolvedTime).atZone(ZONE_ID).toInstant();
    }

    public static LocalTime toResponseTime(LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            return null;
        }
        LocalTime time = scheduledAt.toLocalTime();
        return time.equals(LocalTime.MIDNIGHT) ? null : time;
    }
}
