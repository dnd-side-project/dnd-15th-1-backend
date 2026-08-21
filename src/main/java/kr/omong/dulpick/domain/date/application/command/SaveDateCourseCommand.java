package kr.omong.dulpick.domain.date.application.command;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SaveDateCourseCommand(
        long version,
        String title,
        LocalDate date,
        LocalTime time,
        List<Long> placeIds
) {

    public SaveDateCourseCommand {
        placeIds = placeIds == null ? List.of() : List.copyOf(placeIds);
    }
}
