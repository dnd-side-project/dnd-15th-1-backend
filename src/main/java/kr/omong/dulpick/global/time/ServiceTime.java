package kr.omong.dulpick.global.time;

import java.time.Instant;
import java.time.LocalDateTime;
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
}
